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
	loop:Signal[Option[Span]],
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
		
		def frame2pixelGuarded(frame:Double):Option[Int]	=
				frame2pixel(frame) guardBy display
				
		// NOTE clipping fails for extreme values
		// pixel > -16384 && pixel < 16384 guard {
		private def display(pixel:Int):Boolean	=
				pixel >= (leftX		- WaveUI.maxFigureWidth) &&
				pixel <= (rightX	+ WaveUI.maxFigureWidth)
				
		def span2pixelsGuarded(span:Span):Option[(Int,Int)]	=
				(frame2pixel(span.start), frame2pixel(span.end)) guardBy (display2 _).tupled
				
		private def display2(startPixel:Int, endPixel:Int):Boolean	=
				startPixel	<= leftX	&&
				endPixel	>= rightX	||
				display(startPixel)		||
				display(endPixel)
				
		def goodSize:Boolean	=
				sizeX > 0 && sizeY > 0
	}
	
	//------------------------------------------------------------------------------
	
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
	
	// TODO ugly, boppel rects are already calculated elsewhere
	private val cuepointViews:Signal[ISeq[(Shape,Double)]]	=
			signal {
				val coordsCur	= coords.current
				for {
					cuePoint	<- cuePoints.current
					shape		<- rectangleBoppelShape(coordsCur, cuePoint)
				}
				yield (shape, cuePoint)
			}
			
	val jumpCuepoint:Events[Double]	= 
			((mouse.leftPress snapshotWith cuepointViews) { (ev, views) =>
				views collapseFirst { 
					case (shape, frame) => shape contains (ev.getX, ev.getY) guard frame
				}
			})
			.filterOption
			
	val jumpOutside:Events[Double]	= 
			(mouse.leftPress orElse mouse.leftDrag snapshotWith coords) {
				_.getX |> _.pixel2frame 
			}
	
	val jump:Events[Double]	=
			jumpCuepoint orElse jumpOutside
			
	//------------------------------------------------------------------------------
	//## decorations
	
	private val figures:Signal[ISeq[Figure]]	=
			signal {
				val coordsCur	= coords.current
				
				val loopFigures:ISeq[Figure]	=
						loop.current.toVector flatMap { loop =>
							loopSpanFigure(coordsCur, loop).toVector
						}

				val playerPosFigures:ISeq[Figure]	= 
						positionLineFigure(coordsCur, playerPosition.current).toISeq
				
				val rhythmLineFigures:ISeq[Figure]	= 
						rhythmLines.current flatMap {
							case AnchorLine(frame)	=> 
								markerLineFigure(coordsCur, frame).toSeq	++
								rectangleBoppelFigure(coordsCur, frame).toSeq
							case MeasureLine(frame)	=> 
								markerLineFigure(coordsCur, frame).toSeq	++
								triangleBoppelFigure(coordsCur, frame).toSeq
							case BeatLine(frame)	=>
								markerLineFigure(coordsCur, frame).toSeq
						}
						
				val cuePointFigures:ISeq[Figure]	=
						cuePoints.current.zipWithIndex flatMap { case (frame, index) =>
							markerLineFigure(coordsCur, frame).toSeq		++
							rectangleBoppelFigure(coordsCur, frame).toSeq	++
							numberLabelFigure(coordsCur, frame, index).toSeq
						}
						
				loopFigures ++ rhythmLineFigures ++ cuePointFigures ++ playerPosFigures
			}
	
	private def loopSpanFigure(coords:Coords, loop:Span):Option[Figure]	=
			spanShape(coords, loop) map { shape =>
				FillShape(shape, Style.wave.loop.color)
			}
	
	private def positionLineFigure(coords:Coords, frame:Double):Option[Figure]	=
			lineShape(coords, frame) map { shape =>
				StrokeShape(shape, Style.wave.position.color,	Style.wave.position.stroke)
			}
			
	private def markerLineFigure(coords:Coords, frame:Double):Option[Figure]	=
			lineShape(coords, frame) map { shape =>
				StrokeShape(shape, Style.wave.marker.color,		Style.wave.marker.stroke) 
			}
			
	private def rectangleBoppelFigure(coords:Coords, frame:Double):Option[Figure]	=
			rectangleBoppelShape(coords, frame) map { shape =>
				FillShape(shape, Style.wave.marker.color)
			}
	
	private def triangleBoppelFigure(coords:Coords, frame:Double):Option[Figure]	=
			triangleBoppelShape(coords, frame) map { shape =>
				FillShape(shape, Style.wave.marker.color)
			}
	
	private def lineShape(coords:Coords, frame:Double):Option[Shape]	= {
		import coords._
		frame2pixelGuarded(frame) map { pixel => 
			new Line2D.Double(pixel, topY, pixel, bottomY)
		}
	}
	
	private def spanShape(coords:Coords, span:Span):Option[Shape]	= {
		import coords._
		span2pixelsGuarded(span) map { case (startPixel, endPixel) =>
			new Rectangle2D.Double(startPixel, topY, endPixel-startPixel, bottomY)
		}
	}
	
	private def triangleBoppelShape(coords:Coords, frame:Double):Option[Shape]	= {
		import coords._
		frame2pixelGuarded(frame) map { pixel => 
			val size	= Style.wave.marker.triangle.size
			val left	= pixel - size/2
			val top		= topY
			val middle	= top + size/2
			val bottom	= top + size
			new Polygon(
					Array[Int](pixel,	left,	pixel),
					Array[Int](top,		middle,	bottom),
					3)
		}
	}
	
	private def rectangleBoppelShape(coords:Coords, frame:Double):Option[Shape]	= {
		import coords._
		frame2pixelGuarded(frame) map { pixel => 
			new Rectangle(
					pixel - Style.wave.marker.rectangle.width,
					topY,
					Style.wave.marker.rectangle.width,
					Style.wave.marker.rectangle.height)
		}
	}
	
	private def numberLabelFigure(coords:Coords, frame:Double, number:Int):Option[Figure]	= {
		import coords._
		numberImages lift number flatMap { image =>
			frame2pixelGuarded(frame) map { pixel =>
				val size	= Style.wave.marker.rectangle.width
				val left	= pixel - size + 1
				val top		= topY + 1
				DrawImage(image, left, top)
			}
		}
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
	//## repaint
	
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
}
