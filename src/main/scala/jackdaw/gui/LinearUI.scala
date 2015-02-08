package jackdaw.gui

import java.awt.{ List=>AwtList, Canvas=>JCanvas, _ }
import javax.swing._

import scutil.lang._
import scutil.implicits._

import screact._
import scgeom._
import sc2d._

import jackdaw.gui.util._

/** a potentiometer like linear fader */
final class LinearUI(value:Signal[Double], minimum:Double, maximum:Double, neutral:Option[Double], vertical:Boolean) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val canvas	= new CanvasWrapper(None, Hints.lowQuality)
	
	val component:JComponent	= canvas.component
	
	//------------------------------------------------------------------------------
	//## input
	
	private val orientation	= SgOrientation trueVertical vertical
	private val miniMax		= SgSpan(minimum, maximum)
	
	private val epsilon		= 1.0 / 10000000000D
	private val inMiniMax	= GeomUtil containsInclusive (miniMax, epsilon)
	
	private def clampMiniMax(raw:Double):Double	=
			GeomUtil clampValue (miniMax, raw)
	
	private val trackBounds		=
			canvas.bounds map {
				_
				.modify (orientation,			it => GeomUtil	spanDeflate	(it,		Style.linear.knob.size))
				.modify (orientation.opposite,	it => SgSpan	centerBy 	(it.center,	Style.linear.track.size))
			}
	
	private val value2gui	=
			trackBounds map { it =>
				(orientation cata (miniMax, miniMax.swap))	spanTransformTo 
				(it get orientation)
			}
	
	//------------------------------------------------------------------------------
	//## figures
		
	private val figures:Signal[ISeq[Figure]]	=
			signal {
				val componentBoundsCur	= canvas.bounds.current
				val trackBoundsCur		= trackBounds.current
				val value2guiCur		= value2gui.current
				val valueCur			= value.current
				
				val track	= stripeShape(trackBoundsCur, miniMax, value2guiCur) 
				val active	= neutral map { neutralVal =>
					val	span	= neutralVal spanTo clampMiniMax(valueCur)
					stripeShape(trackBoundsCur, span, value2guiCur)
				}
				val knob	= inMiniMax(valueCur) guard knobShape(componentBoundsCur, valueCur, value2guiCur)
				
				Vector(
					Some(			FillShape(track,	Style.linear.track.color)									),
					active	map {	FillShape(_,		Style.linear.active.color)									},
					Some(			StrokeShape(track,	Style.linear.outline.color,	Style.linear.outline.stroke)	),
					knob	map {	FillShape(_,		Style.linear.knob.color)									},        
					knob	map {	StrokeShape( _,		Style.linear.outline.color,	Style.linear.outline.stroke)	}
				).collapse
			}
			
	private def stripeShape(bounds:SgRectangle, span:SgSpan, value2gui:SgSpanTransform):Shape	=
			GeomUtil smallerRectangle (
					bounds set (
							orientation,
							value2gui transformSpan span))
	
	private def knobShape(bounds:SgRectangle, value:Double, value2gui:SgSpanTransform):Shape	=
			GeomUtil smallerRectangle (
					bounds set (
							orientation,
							value2gui apply value spanCenterBy Style.linear.knob.size))
	
	//------------------------------------------------------------------------------
	//## wiring
	
	figures observe canvas.figures.set
	
	//------------------------------------------------------------------------------
	//## mouse
	
	private val mouseModify:Events[Double] =
			(canvas.mouse.leftPress orElse canvas.mouse.leftDrag snapshotWith value2gui) { (ev, value2gui) =>
				ev.getPoint.toSgPoint get orientation into value2gui.inverse.apply
			}

	private val mouseReset:Events[Double]	=
			canvas.mouse.rightPress filterMap constant(neutral)
	
	private val mouseWheel:Events[Int]	=
			canvas.mouse.wheelRotation map { _ * (miniMax.normal cata (-1, +1)) }
	
	//------------------------------------------------------------------------------
	//## output
	
	val wheel:Events[Int]	= mouseWheel
	
	val changes:Events[Double]	=
			mouseModify	orElse
			mouseReset	map
			clampMiniMax
}
