package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import java.awt.image._

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.geom._
import scutil.math._

import jackdaw.audio.BandCurve
import jackdaw.util.LRU
import jackdaw.gui.util.ImageUtil

final class WaveRenderer(curve:BandCurve, imageUtil:ImageUtil) {
	private var fullImageCache:Option[(IntPoint,BufferedImage)]	= None
	
	def drawFully(g:Graphics2D, target:IntRect) {
		val size	= target.size
		val image	= provideFullImage(size)
		g drawImage (
				image,
				target.x.start,
				target.y.start,
				null)
	}
	
	private def provideFullImage(size:IntPoint):BufferedImage	=
			fullImageCache collect { case (`size`, image) => image } match {
				case Some(image)	=>
					image
				case None	=>
					renderScaled(curve, size) doto { image =>
						fullImageCache	= Some((size, image))
					}
			}
	
	//------------------------------------------------------------------------------
	
	// NOTE this corresponds to one lobe of the blur kernel
	private val tileInset	= 1
	private val tileSize	= 64
	private val tileOffset	= tileSize - 2*tileInset
	private val tileCount	= (curve.chunkCount + tileOffset - 1) / tileOffset
	// derived from a maximum WaveUI width of 4096
	private val tileCached	= 4096 / tileSize
	
	private var tileImageCache:Option[(Int,LRU[Int,BufferedImage])]	= None
	
	def drawPartial(g:Graphics2D, target:IntRect, offset:Int) {
		val x		= target.x.start
		val y		= target.y.start
		val width	= target.x.size
		val height	= target.y.size
		
		// which tile first
		var ti	= 
				if (offset < 0)	(offset - tileOffset + 1) / tileOffset
				else			offset / tileOffset
		// where in the tile
		val tm	= moduloInt(offset, tileOffset)
		
		var xx	= x
		while (xx - tm < x + width) {
			provideTileImage(height, ti) foreach { tile =>
				// first visible, tile's first real (inset) pixel is painted here
				val tilePos	= xx - tm
				// first physical pixel of the tile would be painted here, but is clipped away
				val drawPos	= tilePos - tileInset
				
				// clip away tileInset
				val oldClip	= g.getClip
				g clipRect (tilePos, 0, tileOffset, height)
				g drawImage (tile, drawPos, y, null)
				g  setClip oldClip
			}
				
			// next tile
			xx	+= tileOffset
			ti	+= 1
		}
	}
	
	private def provideTileImage(height:Int, index:Int):Option[BufferedImage]	=
			index guardBy { it => it >= 0 && it < tileCount } map { index =>
				// erstmal: dafür sorgen, daß die map da ist
				// dann: dafür sorgen, daß die map die tiles beinhaltet
				val lru:LRU[Int,BufferedImage]	=
						tileImageCache 
						.collect {
							case (`height`, lru) => lru 
						} 
						.getOrElse {
							new LRU(
									tileCached,
									renderTile(curve, height, _))
							
						}
				tileImageCache	= Some(height, lru)
				lru load index
			}
	
	//------------------------------------------------------------------------------
		
	private def renderTile(curve:BandCurve, height:Int, index:Int):BufferedImage	=
			blur(imageUtil renderImage (IntPoint(tileSize, height), true, g => {
				val sizeY	= height-1
				val bottomY	= height-1
				var x		= 0
				var c		= index * tileOffset - tileInset
				while (x < tileSize) {
					if (c >= 0 && c < curve.chunkCount) {
						val valueLow	= curve	valuesLow 		c
						val valueMiddle	= curve	valuesMiddle	c
						val valueHigh	= curve	valuesHigh		c
						renderLine(
							g, x, sizeY, bottomY,
							valueLow, valueMiddle, valueHigh
						)
					}
					x	+= 1
					c	+= 1
				}
			}))
			
	/*
	private def renderOne(curve:BandCurve, height:Int):BufferedImage	=
			blur(ImageUtil renderImage (IntPoint(curve.chunkCount, height), true, g => {
				val sizeY	= height-1
				val bottomY	= height-1
				var	x	= 0
				while (x < curve.chunkCount) {
					val valueLow	= curve	valuesLow 		x
					val valueMiddle	= curve	valuesMiddle	x
					val valueHigh	= curve	valuesHigh		x
					
					renderLine(
						g, x, sizeY, bottomY,
						valueLow, valueMiddle, valueHigh
					)
				
					x	+= 1
				}
			}))
	*/
			
	private def renderScaled(curve:BandCurve, size:IntPoint):BufferedImage	=
			blur(imageUtil renderImage (size, true, g => {
				val sizeY	= size.y-1
				val bottomY	= size.y-1
				val width	= size.x
				val step	= curve.chunkCount.toDouble * curve.rasterFrames.toDouble / (width+1).toDouble
				var x	= 0
				while (x < width) {
					val	startX	= floor((x+0) * step).toInt
					val	endX	= floor((x+1) * step).toInt
					val sizeX	= endX - startX
					
					val valueLow	= curve rangeLow	(startX, sizeX)
					val valueMiddle	= curve rangeMiddle	(startX, sizeX)
					val valueHigh	= curve rangeHigh	(startX, sizeX)
					
					renderLine(
						g, x, sizeY, bottomY,
						valueLow, valueMiddle, valueHigh
					)
				
					x	+= 1
				}
			}))
			
	// dispatcher
	@inline
	private def renderLine(
		g:Graphics2D, 
		x:Int, sizeY:Int, bottomY:Int, 
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		renderLineColoredSum(	g, x, sizeY, bottomY, valueLow, valueMiddle, valueHigh)
		// renderLineOverlapBands(	g, x, sizeY, bottomY, valueLow, valueMiddle, valueHigh)
	}
	
	// colored energy variant
	@inline
	private def renderLineColoredSum(
		g:Graphics2D, 
		x:Int, sizeY:Int, bottomY:Int, 
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		val color		= sumColor(valueLow, valueMiddle, valueHigh)
		val valueSum	= (valueLow + valueMiddle + valueHigh)
		val ySum		= (valueSum * sizeY).toInt
		
		g setPaint	color
		g drawLine	(x, bottomY-ySum, x, bottomY)
	}
	
	private def sumColor(low:Float, middle:Float, high:Float):Color	= {
		val max	= max3Float(low, middle, high)
		new Color(
			low		/ max, 
			middle	/ max,
			high	/ max
		)
	}
	
	// overwritten band energies variant
	@inline
	private def renderLineOverlapBands(
		g:Graphics2D, 
		x:Int, sizeY:Int, bottomY:Int, 
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		val yLow	= (valueLow		* sizeY).toInt
		val yMiddle	= (valueMiddle	* sizeY).toInt
		val yHigh	= (valueHigh	* sizeY).toInt
		
		g setPaint	Style.wave.overlap.low
		g drawLine	(x, bottomY-yLow,		x, bottomY)
		g setPaint	Style.wave.overlap.middle
		g drawLine	(x, bottomY-yMiddle,	x, bottomY)
		g setPaint	Style.wave.overlap.high
		g drawLine	(x, bottomY-yHigh,		x, bottomY)
	}
	
	//------------------------------------------------------------------------------
	
	private val blurOperation	= {
    	val raw	= Array[Float](
				0,  3, 0,
				1,  7, 1,
				2, 15, 2,
				1,  7, 1,
				0,  3, 0)
		val kernel	= new Kernel(
				3, 5,
				raw map { _ / raw.sum })
		val	operation	= new ConvolveOp(
				kernel, 
				ConvolveOp.EDGE_NO_OP, 
				null)
		operation
    }
    
    private def blur(src:BufferedImage):BufferedImage	=
			blurOperation filter (src, null)
}
