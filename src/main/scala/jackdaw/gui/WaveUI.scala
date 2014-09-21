package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import java.awt.event._
import java.awt.geom._
import java.awt.image.BufferedImage

import scala.math._

import scutil.lang.ISeq
import scutil.implicits._
import scutil.geom._

import screact._
import sc2d._
import scgeom._

import jackdaw.Config
import jackdaw.audio.BandCurve
import jackdaw.model._
import jackdaw.gui.util._

object WaveUI {
	val	maxFigureWidth = 10
}

/** displays curves of a track and emits navigation events */
final class WaveUI(
	bandCurve:Signal[Option[BandCurve]],
	frameOrigin:Signal[Double], 
	playerPosition:Signal[Double],
	cuePoints:Signal[ISeq[Double]],
	rhythmLines:Signal[ISeq[RhythmLine]],
	widthOrigin:Double,	// 0..1 from left to right
	shrink:Boolean
) extends UI with Observing {
	val component	= new PaintedComponent(paintComponent)
	// component setBackground Style.STRONG_BACKGROUND
	component setOpaque	true 
	component setBorder	Style.wave.border
	
	// NOTE this is a def, the ImageUtil is different once the component has a parent
	private def imageUtil	= ImageUtil forComponent component
		
	//------------------------------------------------------------------------------
	
	private val innerRect:Signal[IntRect]	= 
			ComponentUtil innerIntRectSignal component
	
	private val waveRenderer:Signal[Option[WaveRenderer]]	=
			signal { bandCurve.current map { new WaveRenderer(_, imageUtil) } }
	
	private val frameCount:Signal[Int]	=
			signal { bandCurve.current cata (0, _.frameCount) }
			
	// NOTE hacks for zero sized component and missing curve
	private val zoom:Signal[Double]	= shrink cata (
			signal {
				(bandCurve.current cata (Config.curveRaster, _.rasterFrames)).toDouble
			},
			signal {
				innerRect.current.x.size guardBy { _ != 0 } cata (1, frameCount.current / _)
			})
	
	private val coords:Signal[Coords]	= 
			signal { 
				new Coords(innerRect.current, zoom.current, frameOrigin.current) 
			}
	
	private final case class Coords(val inner:IntRect, zoomFactor:Double, frameOrig:Double) {
		val leftX	= inner.x.start
		val rightX	= inner.x.end
		val topY	= inner.y.start
		val bottomY	= inner.y.end
		
		val sizeX	= inner.x.size
		val sizeY	= inner.y.size
		
		def src(pixel:Int):Int	= round(pixel2frame(pixel) / Config.curveRaster).toInt
		 
		private val pixelOrig	= leftX + (sizeX * widthOrigin).toInt
		def pixel2frame(pixel:Int):Double	= (pixel - pixelOrig) * zoomFactor + frameOrig
		def frame2pixel(frame:Double):Int	= round((frame - frameOrig) / zoomFactor + pixelOrig).toInt
		def value2y(value:Double):Int		= (bottomY - value * sizeY).toInt
		
		def display(pixel:Int):Boolean	=
				pixel >= (leftX		- WaveUI.maxFigureWidth) &&
				pixel <= (rightX	+ WaveUI.maxFigureWidth)
				
		def goodSize:Boolean	=
				sizeX > 0 && sizeY > 0
	}
	
	//------------------------------------------------------------------------------
	
	private val mouse	= new Mouse(component)
	
	val jump:Events[Double]	= 
			(mouse.leftPress orElse mouse.leftDrag snapshotWith coords) {
				_.getX |> _.pixel2frame 
			}
	
	private val scratchRelative:Events[Double]	=
			for {
				press	<- mouse.leftPress
				zoom1 	= zoom.current
				drag	<- mouse.leftDrag
			}
			yield zoom1 * (drag.getX - press.getX) 
			
	val scratchFrame:Signal[Option[Double]]		= 
			mouse.leftPress		tag
			0.0					orElse
			scratchRelative		sum
			mouse.leftRelease	map
			{ _.left.toOption }	hold
			None
	
	val playToggle:Events[Unit]	= 
			mouse.rightPress tag (())
	
	val seek:Events[Int]	=
			mouse.wheelRotation
	
	//------------------------------------------------------------------------------
	
	private val figures:Signal[ISeq[Figure]]	=
			signal {
				val coordsCur	= coords.current
				
				val playerPosFigures:ISeq[Figure]	= 
						renderLine(coordsCur, playerPosition.current, true).toISeq
				
				val rhythmLineFigures:ISeq[Figure]	= 
						rhythmLines.current flatMap {
							case RhythmLine.AnchorLine(frame)	=> 
								renderLine(coordsCur, frame, false).toSeq	++
								renderRectangleBoppel(coordsCur, frame).toSeq
							case RhythmLine.MeasureLine(frame)	=> 
								renderLine(coordsCur, frame, false).toSeq	++
								renderTriangleBoppel(coordsCur, frame).toSeq
							case RhythmLine.BeatLine(frame)	=>
								renderLine(coordsCur, frame, false).toSeq
						}
				
				val cuePointFigures:ISeq[Figure]	=
						cuePoints.current.zipWithIndex flatMap { case (frame,index) =>
							renderLine(coordsCur, frame, false).toSeq		++
							renderRectangleBoppel(coordsCur, frame).toSeq	++
							renderNumberLabel(coordsCur, frame, index).toSeq
						}
				
				rhythmLineFigures ++ cuePointFigures ++ playerPosFigures
			}
	
	private def renderLine(coords:Coords, frame:Double, position:Boolean ):Option[Figure]	= {
		import coords._
		val	pixel	= frame2pixel(frame)
		// NOTE clipping fails for extreme values
		//pixel > -16384 && pixel < 16384 guard {
		coords display pixel guard {
			val shape	= new Line2D.Double(pixel, topY, pixel, bottomY)
			if (position)	StrokeShape(shape, Style.wave.position.color, Style.wave.position.stroke)
			else			StrokeShape(shape, Style.wave.marker.color, Style.wave.marker.stroke) 
		}
	}
	
	private def renderTriangleBoppel(coords:Coords, frame:Double):Option[Figure]	= {
		import coords._
		val	pixel	= frame2pixel(frame)
		// NOTE clipping fails for extreme values
		// pixel > -16384 && pixel < 16384 guard {
		coords display pixel guard {
			val size	= Style.wave.marker.triangle.size
			val left	= pixel - size/2
			val top		= topY
			val middle	= top + size/2
			val bottom	= top + size
			val shape	= new Polygon(
						Array[Int](pixel, left, pixel),
						Array[Int](top, middle, bottom),
						3)
			FillShape(shape, Style.wave.marker.color)
		}
	}
	
	private def renderRectangleBoppel(coords:Coords, frame:Double):Option[Figure]	= {
		import coords._
		val	pixel	= frame2pixel(frame)
		// NOTE clipping fails for extreme values
		// pixel > -16384 && pixel < 16384 guard {
		coords display pixel guard {
			val left	= pixel - Style.wave.marker.rectangle.width
			val top		= topY
			val bottom	= top + Style.wave.marker.rectangle.height
			val shape	= new Polygon(
					Array[Int](pixel, left, left, pixel),
					Array[Int](top, top, bottom, bottom),
					4)
			FillShape(shape, Style.wave.marker.color)
		}
	}
	
	private def renderNumberLabel(coords:Coords, frame:Double, number:Int):Option[Figure]	= {
		import coords._
		val	pixel	= frame2pixel(frame)
		// NOTE clipping fails for extreme values
		// pixel > -16384 && pixel < 16384 && number >= 0 && number < numberImages.size guard {
		(coords display pixel) && number >= 0 && number < numberImages.size guard {
			val image	= numberImages(number)
			val size	= Style.wave.marker.rectangle.width
			val left	= pixel - size + 1
			val top		= topY + 1
			DrawImage(image, left, top)
		}
	}
	
	private lazy val numberImages:ISeq[BufferedImage]	= {
		// TODO hardcoded insets
		val size	= IntPoint(Style.wave.marker.rectangle.width-1, Style.wave.marker.rectangle.height-2)
		val bounds	= SgRectangle topLeftZeroBy SgPoint(size.x-1, size.y-1)
		LEDShape shapes bounds map { shape	=>
			val figure	= StrokeShape(shape, Style.wave.marker.number.color, Style.wave.marker.number.stroke)
			imageUtil renderImage (size, true, figure.paint)
		}
	}
	
	// repaint everything on size, origin or source changes
	private val fullRepaints	= 
			innerRect.edge		orElse 
			bandCurve.edge		orElse
			frameOrigin.edge	tag	
			Vector(new Rectangle(component.getSize()))
	
	// repaint figures when they change in any way
	private val partialRepaints	=
			figures slide { (older, newer) => 
				((older ++ newer) map { _.bounds.getBounds }).distinct
			}
			
	// full repaints coalesce partial repaints
	private val repaints	= 
			fullRepaints orElse partialRepaints
	
	repaints observe { _ foreach component.repaint }
	// completeRepaints observe { _ => component.paintImmediately(0,0,component.getWidth,component.getHeight) }

	//------------------------------------------------------------------------------
	//## painting
	
	private def paintComponent(g:Graphics2D) {
		// g setRenderingHint (RenderingHints.KEY_ANTIALIASING, 		RenderingHints.VALUE_ANTIALIAS_ON)
		// g setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING,	RenderingHints.VALUE_TEXT_ANTIALIAS_ON)	// VALUE_TEXT_ANTIALIAS_GASP
		// g setRenderingHint (RenderingHints.KEY_INTERPOLATION,		RenderingHints.VALUE_INTERPOLATION_BICUBIC)
		// g setRenderingHint (RenderingHints.KEY_COLOR_RENDERING,		RenderingHints.VALUE_COLOR_RENDER_QUALITY)
		// g setRenderingHint (RenderingHints.KEY_ALPHA_INTERPOLATION,	RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)

		val clipBounds	= g.getClipBounds
		
		// background
		g setPaint Style.wave.background.color
		g fill clipBounds
		
		val coord	= coords.current; import coord._
		
		// NOTE if very small the inner size might become negative
		if (goodSize) {
			// curves
			// val beginX	= clipBounds.x
			// val endX	= clipBounds.x+clipBounds.width
			for (waveRenderer <- waveRenderer.current) {
				// g setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
				// g setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
				// g setRenderingHint (RenderingHints.KEY_RENDER_QUALITY, RenderingHints.VALUE_RENDER_QUALITY)
	
				if (shrink)	waveRenderer drawFully		(g, inner)
				else		waveRenderer drawPartial	(g, inner, src(leftX))
			}
			
			// pre-roll
			val before	= frame2pixel(0)
			if (before > leftX) {
				g setPaint Style.wave.roll.paint
				g fillRect (leftX, bottomY-Style.wave.roll.height, before-leftX, Style.wave.roll.height)
			}
		
			// post-roll
			val after	= frame2pixel(frameCount.current)
			if (after < rightX) {
				g setPaint Style.wave.roll.paint
				g fillRect (after, bottomY-Style.wave.roll.height, rightX-after, Style.wave.roll.height)
			}
			
			// figures
			figures.current foreach { _ paint g }
		}
	}

	/*
	private def paintCurve(g:Graphics2D, coord:Coords, beginX:Int, endX:Int, curve:BandCurve) {
		import coord._
		var x	= beginX
		while (x < endX) {
			val	beginInt	= round(pixel2frame(x)).toInt
			val zoomInt		= zoomFactor.toInt
			
			val valueLow	= curve rangeLow	(beginInt, zoomInt)
			val valueMiddle	= curve rangeMiddle (beginInt, zoomInt)
			val valueHigh	= curve rangeHigh	(beginInt, zoomInt)
			
			paintLine(
				g, 
				coord, x,
				valueLow, valueMiddle, valueHigh
			)
			
			x	+= 1
		}
	}
	
	// dispatcher
	@inline
	private def paintLine(
		g:Graphics2D, 
		coord:Coords, x:Int,
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		paintLineColoredSum(	g, coord, x, valueLow, valueMiddle, valueHigh)
		// paintLineOverlapBands(	g, coord, x, valueLow, valueMiddle, valueHigh)
		// paintLineStackedBands(	g, coord, x, valueLow, valueMiddle, valueHigh)
	}
	
	// colored energy variant
	@inline
	private def paintLineColoredSum(
		g:Graphics2D,
		coord:Coords, x:Int,
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		import coord.{ value2y, bottomY }
		
		val valueSum	= (valueLow + valueMiddle + valueHigh)
		val ySum		= value2y(valueSum)
		val valueMax	= max3(valueLow, valueMiddle, valueHigh)
		val color		= new Color(valueLow/valueMax, valueMiddle/valueMax, valueHigh/valueMax)
		g setPaint	color
		g drawLine (x, ySum, x, bottomY)
	}
	
	// overwritten band energies variant
	@inline
	private def paintLineOverlapBands(
		g:Graphics2D,
		coord:Coords, x:Int,
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		import coord.{ value2y, bottomY }
		
		// BETTER scale up, these values are usually smaller than 1.0f
		val yLow	= value2y(valueLow)
		val yMiddle	= value2y(valueMiddle)
		val yHigh	= value2y(valueHigh)
		g setPaint	Style.wave.overlap.low
		g drawLine (x, yLow,	x, bottomY)
		g setPaint	Style.wave.overlap.middle
		g drawLine (x, yMiddle,	x, bottomY)
		g setPaint	Style.wave.overlap.high
		g drawLine (x, yHigh,	x, bottomY)
	}
	
	// stacked band energies variant
	@inline
	private def paintLineStackedBands(
		g:Graphics2D, coord:Coords, x:Int,
		valueLow:Float, valueMiddle:Float, valueHigh:Float
	) {
		import coord.{ value2y, bottomY }
		
		// NOTE gain2fader and addition don't mix well 
		val yLow	= value2y(valueLow)
		val yMiddle	= value2y(valueMiddle)
		val yHigh	= value2y(valueHigh)
		val y0		= bottomY
		val y1		= yLow
		val y2		= y1 - (bottomY - yMiddle)
		val y3		= y2 - (bottomY - yHigh)
		g setPaint	Style.wave.stacked.low
		g drawLine (x, y1, x, y0)
		g setPaint	Style.wave.stacked.middle
		g drawLine (x, y2, x, y1)
		g setPaint	Style.wave.stacked.high
		g drawLine (x, y3, x, y2)
	}
	*/
}
