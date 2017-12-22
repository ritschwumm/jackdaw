package jackdaw.gui

import java.awt.{ List=>_, _ }
import java.awt.geom._
import java.awt.image.BufferedImage

import scala.math._

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.lang._
import scutil.geom._
import scutil.gui.implicits._

import screact._
import sc2d._
import scgeom._

import jackdaw.Config
import jackdaw.curve.BandCurve
import jackdaw.data._
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
	rhythmAnchor:Signal[Option[Double]],
	loop:Signal[Option[Span]],
	widthOrigin:Double,	// 0..1 from left to right
	shrink:Boolean
) extends UI with Observing {
	val component	= new PaintedComponent(paintComponent)
	// component setBackground Style.STRONG_BACKGROUND
	component setOpaque	true
	component setBorder	Style.wave.border
	
	component setDoubleBuffered false
	
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
				innerRect.current.width optionBy { _ != 0 } cata (1, frameCount.current / _)
			})
	
	private val coords:Signal[Coords]	=
			signal {
				new Coords(innerRect.current, zoom.current, frameOrigin.current)
			}
	
	private sealed case class Coords(val inner:IntRect, zoomFactor:Double, frameOrig:Double) {
		val leftX	= inner.left
		val rightX	= inner.right
		val topY	= inner.top
		val bottomY	= inner.bottom
		val sizeX	= inner.width
		val sizeY	= inner.height
		
		def src(pixel:Int):Int	= round(pixel2frame(pixel) / Config.curveRaster).toInt
		
		private val pixelOrig	= leftX + (sizeX * widthOrigin).toInt
		def pixel2frame(pixel:Int):Double	= (pixel - pixelOrig) * zoomFactor + frameOrig
		def frame2pixel(frame:Double):Int	= round((frame - frameOrig) / zoomFactor + pixelOrig).toInt
		def value2y(value:Double):Int		= (bottomY - value * sizeY).toInt
		
		def frame2pixelGuarded(frame:Double):Option[Int]	=
				frame2pixel(frame) optionBy display
				
		// NOTE clipping fails for extreme values
		// pixel > -16384 && pixel < 16384 option {
		private def display(pixel:Int):Boolean	=
				pixel >= (leftX		- WaveUI.maxFigureWidth) &&
				pixel <= (rightX	+ WaveUI.maxFigureWidth)
				
		def span2pixelsGuarded(span:Span):Option[(Int,Int)]	=
				(frame2pixel(span.start), frame2pixel(span.end)) optionBy (display2 _).tupled
				
		private def display2(startPixel:Int, endPixel:Int):Boolean	=
				startPixel	<= leftX	&&
				endPixel	>= rightX	||
				display(startPixel)		||
				display(endPixel)
				
		def goodSize:Boolean	=
				sizeX > 0 && sizeY > 0
	}
	
	//------------------------------------------------------------------------------
	//## decorations
	
	private type Jump	= (Figure, Double)
	
	private val figuresAndJumps:Signal[(ISeq[Figure],ISeq[Jump])]	=
			signal {
				val decorator	= new Decorator(coords.current)
				
				val rollFigures:ISeq[Figure]	=
						decorator.preRollFigure.toVector ++
						decorator.postRollFigure.toVector
					
				val loopFigures:ISeq[Figure]	=
						loop.current.toISeq collapseMap decorator.loopSpanFigure

				val playerPositionFigures:ISeq[Figure]	=
						(decorator positionLineFigure playerPosition.current).toISeq
				
				val rhythmLineFigures:ISeq[Figure]	=
						rhythmLines.current flatMap { case RhythmLine(frame, unit) =>
							val line	= decorator markerLineFigure	frame
							val boppel	=
									unit match {
										case Phrase		=> decorator sixangleBoppelFigure	frame
										case Measure	=> decorator triangleBoppelFigure	frame
										case Beat		=> None
									}
							flatSeq(line.toVector, boppel.toVector)
						}
						
				val rhythmAnchorClickables:Option[(ISeq[Figure],Option[Jump])]	=
						rhythmAnchor.current map { frame =>
							val line	= decorator markerLineFigure		frame
							val boppel	= decorator rectangleBoppelFigure	frame
							val figures	= flatSeq(line.toVector, boppel.toVector)
							val action	= boppel map { _ -> frame }
							(figures, action)
						}
						
				val rhythmAnchorFigures:ISeq[Figure]	=
						(rhythmAnchorClickables map { _._1 }).flattenMany
					
				val rhythmAnchorJumps:ISeq[Jump]	=
						(rhythmAnchorClickables map { _._2 }).flatten.toISeq
						
				val cuePointClickables:ISeq[(ISeq[Figure],Option[Jump])]	=
						cuePoints.current.zipWithIndex map { case (frame, index) =>
							val line	= decorator markerLineFigure		frame
							val boppel	= decorator rectangleBoppelFigure	frame
							val label	= decorator numberLabelFigure		(frame, index)
							val figures	= flatSeq(line.toVector, boppel.toVector, label.toVector)
							val action	= boppel map { _ -> frame }
							(figures, action)
						}
						
				val cuePointFigures:ISeq[Figure]	=
						(cuePointClickables map { _._1 }).flatten
					
				val cuePointJumps:ISeq[Jump]	=
						(cuePointClickables map { _._2 }).collapse
						
				val figures:ISeq[Figure]	= rollFigures ++ loopFigures ++ rhythmLineFigures ++ rhythmAnchorFigures ++ cuePointFigures ++ playerPositionFigures
				val jumps:ISeq[Jump]		= rhythmAnchorJumps ++ cuePointJumps
				
				(figures, jumps)
			}
			
	private val (figures, jumps)	= figuresAndJumps.unzip
	typed[Signal[ISeq[Figure]]](figures)
	typed[Signal[ISeq[Jump]]](jumps)
			
	private def flatSeq[T](its:ISeq[T]*):ISeq[T]	=
			its.toVector.flatten
			
	private final class Decorator(coords:Coords) {
		def preRollFigure:Option[Figure]	=
				coords frame2pixel 0 optionBy { _ > coords.leftX } map { before =>
					val shape	= new Rectangle2D.Double(coords.leftX, coords.bottomY-Style.wave.roll.height, before-coords.leftX, Style.wave.roll.height)
					FillShape(shape, Style.wave.roll.paint)
				}
			
		def postRollFigure:Option[Figure]	=
				coords frame2pixel frameCount.current optionBy { _ < coords.rightX } map { after =>
					val shape	= new Rectangle2D.Double(after, coords.bottomY-Style.wave.roll.height, coords.rightX-after, Style.wave.roll.height)
					FillShape(shape, Style.wave.roll.paint)
				}
				
		def loopSpanFigure(loop:Span):Option[Figure]	=
				coords span2pixelsGuarded loop map { case (startPixel, endPixel) =>
					val shape	= spanShape(startPixel, endPixel)
					FillShape(shape, Style.wave.loop.color)
				}
		
		def positionLineFigure(frame:Double):Option[Figure]	=
				coords frame2pixelGuarded frame map { pixel =>
					val shape	= lineShape(pixel)
					StrokeShape(shape, Style.wave.position.color,	Style.wave.position.stroke)
				}
				
		def markerLineFigure(frame:Double):Option[Figure]	=
				coords frame2pixelGuarded frame map { pixel =>
					val shape	= lineShape(pixel)
					StrokeShape(shape, Style.wave.marker.color,		Style.wave.marker.stroke)
				}
				
		def rectangleBoppelFigure(frame:Double):Option[Figure]	=
				coords frame2pixelGuarded frame map { pixel =>
					val shape	= rectangleBoppelShape(pixel)
					FillShape(shape, Style.wave.marker.color)
				}
		
		def triangleBoppelFigure(frame:Double):Option[Figure]	=
				coords frame2pixelGuarded frame map { pixel =>
					val shape	= triangleBoppelShape(pixel)
					FillShape(shape, Style.wave.marker.color)
				}
		
		def sixangleBoppelFigure(frame:Double):Option[Figure]	=
				coords frame2pixelGuarded frame map { pixel =>
					val shape	= sixangleBoppelShape(pixel)
					FillShape(shape, Style.wave.marker.color)
				}
				
		def numberLabelFigure(frame:Double, number:Int):Option[Figure]	=
				for {
					image	<- numberImages lift number
					pixel	<- coords frame2pixelGuarded frame
				}
				yield {
					val size	= Style.wave.marker.rectangle.width
					val left	= pixel - size + 1
					val top		= coords.topY + 1
					DrawImage(image, left, top)
				}
		
		//------------------------------------------------------------------------------
		
		private def lineShape(pixel:Int):Shape	=
				new Line2D.Double(pixel, coords.topY, pixel, coords.bottomY)

		private def spanShape(startPixel:Int, endPixel:Int):Shape	=
				new Rectangle2D.Double(startPixel, coords.topY, endPixel-startPixel, coords.bottomY)
		
		private def triangleBoppelShape(pixel:Int):Shape	= {
			val width	= Style.wave.marker.triangle.width
			val height	= Style.wave.marker.triangle.height
			val left	= pixel - width
			val top		= coords.topY
			val middle	= top + height/2
			val bottom	= top + height
			new Polygon(
				Array[Int](pixel,	left,	pixel),
				Array[Int](top,		middle,	bottom),
				3
			)
		}
		
		private def sixangleBoppelShape(pixel:Int):Shape	= {
			val width	= Style.wave.marker.sixangle.width
			val height	= Style.wave.marker.sixangle.height
			val left	= pixel - width
			val top		= coords.topY
			val middle	= top + height/2
			val bottom	= top + height
			new Polygon(
				Array[Int](pixel,	left,	pixel),
				Array[Int](top,		middle,	bottom),
				3
			)
		}
		
		private def rectangleBoppelShape(pixel:Int):Shape	=
				new Rectangle(
					pixel - Style.wave.marker.rectangle.width,
					coords.topY,
					Style.wave.marker.rectangle.width,
					Style.wave.marker.rectangle.height
				)
	}
	
	private val numberImages:ISeq[BufferedImage]	= {
		val size	= Style.wave.marker.number.size
		val end		= Style.wave.marker.number.end
		val bounds	= SgRectangle topLeftZeroBy SgPoint(end.x, end.y)
		LEDShape shapes bounds map { shape	=>
			val figure	= StrokeShape(shape, Style.wave.marker.number.color, Style.wave.marker.number.stroke)
			imageUtil renderImage (size, true, figure.paint)
		}
	}
	
	//------------------------------------------------------------------------------
	//## events
	
	private val mouse	= new Mouse(component)
	
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
	
	private val jumpFlag:Events[Double]	=
			((mouse.leftPress snapshotWith jumps) { (ev, jumps) =>
				jumps collapseMapFirst { case (figure, frame) =>
					figure pick ev.getPoint option frame
				}
			})
			.filterOption
			
	private val jumpOutside:Events[Double]	=
			(mouse.leftPress orElse mouse.leftDrag snapshotWith coords) {
				_.getX |> _.pixel2frame
			}
	
	val jump:Events[Double]	=
			jumpFlag orElse jumpOutside
		
	//------------------------------------------------------------------------------
	//## repaint
	
	// repaint everything on size, origin or source changes
	private val fullRepaints:Events[ISeq[Rectangle]]	=
			innerRect.edge		orElse
			bandCurve.edge		orElse
			frameOrigin.edge	tag	
			Vector(new Rectangle(component.getSize()))
	
	// repaint figures when they change in any way
	private val partialRepaints:Events[ISeq[Rectangle]]	=
			figures slide { (older, newer) =>
				((older ++ newer) map { _.bounds.getBounds }).distinct
			}
			
	// full repaints coalesce partial repaints
	private val repaints:Events[ISeq[Rectangle]]	=
			fullRepaints orElse partialRepaints
	
	repaints observe { _ =>
		val g	= component.getGraphics.asInstanceOf[Graphics2D]
		if (g ne null) {
			val oldClip= g.getClip
			g setClip new Rectangle(component.getSize())
			// NOTE paintCompoment(g) works, but doesn't paint the border
			component paint g
			g setClip oldClip
			Toolkit.getDefaultToolkit.sync()
		}
	}
	// repaints observe { _ foreach component.repaint }
	// completeRepaints observe { _ => component.paintImmediately(0,0,component.getWidth,component.getHeight) }

	//------------------------------------------------------------------------------
	//## painting
	
	private def paintComponent(g:Graphics2D) {
		// g setRenderingHint (RenderingHints.KEY_ANTIALIASING, 		RenderingHints.VALUE_ANTIALIAS_ON)
		// g setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING,	RenderingHints.VALUE_TEXT_ANTIALIAS_ON)	// VALUE_TEXT_ANTIALIAS_GASP
		// g setRenderingHint (RenderingHints.KEY_INTERPOLATION,		RenderingHints.VALUE_INTERPOLATION_BICUBIC)
		// g setRenderingHint (RenderingHints.KEY_COLOR_RENDERING,		RenderingHints.VALUE_COLOR_RENDER_QUALITY)
		// g setRenderingHint (RenderingHints.KEY_ALPHA_INTERPOLATION,	RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)

		val coord	= coords.current
		// NOTE if very small the inner size might become negative
		if (coord.goodSize) {
			// renderToGraphics(coord, g)
			renderUsingImage(coord, g)
		}
	}
	
	private def renderUsingImage(coord:Coords, g:Graphics2D) {
		g drawImage (renderToImage(coord), 0, 0, null)
	}
	
	private var bufferImage:BufferedImage	= null
	
	private def renderToImage(coord:Coords):BufferedImage	= {
		import coord._
		
		if (bufferImage == null || bufferImage.getWidth != sizeX || bufferImage.getHeight != sizeY) {
			bufferImage	= imageUtil createImage (inner.size, false)
		}
		bufferImage.createGraphics.asInstanceOf[Graphics2D] use { bg =>
			renderToGraphics(coord, bg)
		}
		
		bufferImage
	}
	
	private def renderToGraphics(coord:Coords, bg:Graphics2D) {
		import coord._
		
		/*
		// background
		val clipBounds	= g.getClipBounds
		g setPaint Style.wave.background.color
		g fill clipBounds
		*/
		
		// background
		bg setPaint Style.wave.background.color
		bg fillRect (leftX, topY, sizeX, sizeY)
		
		// curves
		// val beginX	= clipBounds.x
		// val endX	= clipBounds.x+clipBounds.width
		for (waveRenderer <- waveRenderer.current) {
			// g setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
			// g setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
			// g setRenderingHint (RenderingHints.KEY_RENDER_QUALITY, RenderingHints.VALUE_RENDER_QUALITY)

			if (shrink)	waveRenderer drawFully		(bg, inner)
			else		waveRenderer drawPartial	(bg, inner, src(leftX))
		}
		
		// figures
		figures.current foreach { _ paint bg }
	}
}
