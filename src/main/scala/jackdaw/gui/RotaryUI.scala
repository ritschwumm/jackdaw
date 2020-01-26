package jackdaw.gui

import java.awt.{ List=>_, Canvas=>_, _ }
import java.awt.event._
import java.awt.geom._
import javax.swing._

import scala.math._

import scutil.base.implicits._
import scutil.math.functions._

import screact._
import scgeom._
import sc2d._

import jackdaw.gui.util._

/** a potentiometer like rotary knob */
final class RotaryUI(value:Signal[Double], minimum:Double, maximum:Double, neutral:Double) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components

	private val canvas	= new CanvasWrapper(None, Hints.highQuality)

	val component:JComponent	= canvas.component

	//------------------------------------------------------------------------------
	//## input

	// NOTE rotary goes from 270 to -90
	// 0 degrees is right
	// 90 degrees is top
	// 180 degrees is left and
	// 270 or -90 degrees is bottom
	private val valueSpan		= minimum					spanTo	maximum
	private val angleSpan		= Style.rotary.angle.min	spanTo	Style.rotary.angle.max
	private val valueToAngle	= valueSpan spanTransformTo angleSpan

	private val epsilon		= 1.0 / 10000000000D
	private val inValueSpan	= GeomUtil containsInclusive (valueSpan, epsilon)

	private def clampValueSpan(raw:Double):Double	=
		GeomUtil clampValue (valueSpan, raw)

	//------------------------------------------------------------------------------
	//## figures

	// TODO smaller angle difference for the knob to make it stay inside the track
	private val figures:Signal[Seq[Figure]]	=
		signal {
			val boundsCur	= canvas.bounds.current
			val valueCur	= value.current

			val track	= stripeShape(boundsCur, angleSpan)
			val active	= stripeShape(
					boundsCur,
					valueToAngle(neutral) spanTo valueToAngle(valueCur))

			val knob	= inValueSpan(valueCur) option {
				/*
				// rectangle at the top of the track (that is at 90 degrees)
				val knobRect	= SgRectangle(
						boundsCur.x.center	spanCenterBy	Style.ROTARY_KNOB_SIZE,
						SgSpan				startZeroBy		Style.ROTARY_TRACK_SIZE+1)
				// subtract the 90 degrees we already have by construction of the rect
				val rotation	= valueToAngle(valueCur) - 90
				*/
				// rectangle at the right of the track, at 0 degrees
				val knobRect	=
						(boundsCur.x.end	spanEndBy 		Style.rotary.track.size+1)	rectangleWith
						(boundsCur.y.center	spanCenterBy	Style.rotary.knob.size)
				val rotation	= valueToAngle(valueCur)
				val xform		=
						SgAffineTransform
						.translate	( boundsCur.center)
						.rotate		(toRadians(-rotation))
						.translate	(-boundsCur.center)
				xform transformAwtShape knobRect.toAwtRectangle2D
			}

			Vector(
				Some(		FillShape(track,	Style.rotary.track.color)									),
				Some(		FillShape(active,	Style.rotary.active.color)									),
				Some(		StrokeShape(track,	Style.rotary.outline.color,	Style.rotary.outline.stroke)	),
				knob map {	FillShape(_,		Style.rotary.knob.color)									},
				knob map {	StrokeShape(_,		Style.rotary.outline.color,	Style.rotary.outline.stroke)	}
			).collapse
		}

	private def stripeShape(bounds:SgRectangle, span:SgSpan):Shape = {
		// stroke an open arc with a suffiently fast stroke
		val inset	= bounds inset (SgRectangleInsets.one * Style.rotary.track.size / 2)
		val stroke	= new BasicStroke(
			Style.rotary.track.size,
			BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_ROUND)
		val path	= new Arc2D.Double(
			inset.toAwtRectangle2D,
			span.start,
			span.size,
			Arc2D.OPEN)
		stroke createStrokedShape path

		/*
		// subtract an ellipse from a big pie arc
		val outer	= new Arc2D.Double(
			bounds.toAwtRectangle2D,
			span.start,
			span.size,
			Arc2D.PIE)
		val inset	= bounds inset (SgRectangleInsets.one * Style.ROTARY_TRACK_SIZE)
		val inner	= new Ellipse2D.Double(
			inset.x.start,
			inset.y.start,
			inset.x.size,
			inset.y.size)

		// shape subtraction
		val outerArea	= new Area(outer)
		val innerArea	= new Area(inner)
		outerArea subtract innerArea
		outerArea
		*/
	}

	//------------------------------------------------------------------------------
	//## wiring

	figures observe canvas.figures.set

	//------------------------------------------------------------------------------
	//## mouse

	// BETTER make events and connect them to value instead of calling it deep within

	private val mousePress:Events[MouseEvent]	= canvas.mouse.leftPress
	private val mouseDrag:Events[MouseEvent]	= canvas.mouse.leftDrag

	// mouse actions modify the value
	private val mouseModify:Events[Double] =
		mousePress orElse mouseDrag filterMap { ev =>
			calculateValue(ev.getPoint) combineWith (_ flatOptionNot _)
		}

	private val mouseReset:Events[Double]	=
		mousePress filter
		{ ev => calculateValue(ev.getPoint)._1 }	orElse
		canvas.mouse.rightPress						tag
		neutral

	/** returns whether the mouse is inside the track and a new value if possible */
	private def calculateValue(awtPoint:Point):(Boolean,Option[Double])	= {
		val point	= awtPoint.toSgPoint
		val size	= component.getSize().toSgPoint
		val center	= size / 2
		val offset	= point - center

		val inside		= {
			val distance	= offset.length
			// -2 for outline, /3 instead of /2 for inaccurate mouse
			val inner		= canvas.bounds.current
			val ignore		= ((inner.x.size min inner.y.size) - Style.rotary.track.size - 2) / 3.0
			distance < ignore
		}

		val value	= {
		// we need the jump at the bottom instead of at the left

		// 		-90
		//	+-180	0
		//		+90
		val angle1	= toDegrees(offset.angle)

		// 		+90
		//	+-180	0
		//		-90
		val angle2	= -angle1

		//			90
		//	180				0
		//		270/-90
		val angle3	=
				if (angle2 < -90)	angle2 + 360
				else				angle2

		// half opening overshot is allowed and limited
		(angle3 >= Style.rotary.angle.max - Style.rotary.angle.opening/2) &&
			(angle3 <= Style.rotary.angle.min + Style.rotary.angle.opening/2) option {
				val angle	= clampDouble(angle3, Style.rotary.angle.max, Style.rotary.angle.min)
				valueToAngle inverse angle
			}
		}

		(inside, value)
	}

	private val mouseWheel:Events[Int]	=
		canvas.mouse.wheelRotation map { _ * (valueSpan.normal cata (-1, +1)) }

	//------------------------------------------------------------------------------
	//## output

	val wheel:Events[Int]	= mouseWheel

	val changes:Events[Double]	=
		mouseModify	orElse
		mouseReset	map
		clampValueSpan
}
