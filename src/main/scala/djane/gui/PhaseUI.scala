package djane.gui

import java.awt.{ List=>AwtList, Canvas=>JCanvas, _ }
import java.awt.event._
import javax.swing._

import scala.math._

import scutil.implicits._

import screact._
import scgeom._
import sc2d._

import djane.model._
import djane.gui.util._

object PhaseUI {
	private val subDivide	= Rhythm.defaultBeatsPerMeasure
}

/** values are linear [-.5..+.5] */
final class PhaseUI(value:Signal[Option[Double]], rhythm:Signal[Option[Rhythm]]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val canvas	= new CanvasWrapper(Some(Style.phase.background.color), Hints.lowQuality)
	
	val component:JComponent	= canvas.component
	component setBorder	Style.phase.border
	
	//------------------------------------------------------------------------------
	//## input

	private val miniMax			= SgSpan(-0.5, +0.5)
	
	// NOTE hack to show inverse a little bit more often
	private val epsilon			= -0.0001
	private val insideMiniMax	= GeomUtil containsInclusive (miniMax, epsilon)
		
	private def clampMiniMax(raw:Double):Double	=
			GeomUtil clampValue (miniMax, raw)
	
	private val value2gui	= 
			canvas.bounds map { it =>
				SgSpanTransform fromSpans (miniMax, it.x)
			}
	
	//------------------------------------------------------------------------------
	//## figures
	
	private val figures:Signal[Seq[Figure]]	=
			signal {
				val trackBoundsCur	= canvas.bounds.current
				val value2guiCur	= value2gui.current
				val valueCur		= value.current
				val rhythmCur		= rhythm.current
				val guiNeutral		= value2guiCur(0.0)
				
				// TODO hardcoded insets
				val lineSpan		= trackBoundsCur.y inset SgSpanInsets(2, 3)
				
				val bar	= 
						for {
							value	<- valueCur
							rect	= 
									// extreme values fill complete area
									if (insideMiniMax(value)) {
										// NOTE hack to make it change less often
										rint(value2guiCur(value)) spanTo guiNeutral rectangleWith trackBoundsCur.y
									}
									else {
										trackBoundsCur
									}
							// NOTE hack to shortcut the zero-phase case
							if !rect.empty
						}
						yield {
							val shape	= GeomUtil normalRectangle rect
							FillShape(shape, Style.phase.bar.color)
						}
				
				val lines	= 
						for {
							rhythm		<- rhythmCur.toSeq
							subRaster	= rhythm.beatsPerMeasure * PhaseUI.subDivide
							max			= (subRaster - 1) / 2
							index		<- -max to +max
						}
						yield {
							val shape	= (SgLine vertical (value2guiCur(index.toDouble / subRaster), lineSpan)).toAwtLine2D
							(index % PhaseUI.subDivide) == 0 cata (
								StrokeShape(shape, Style.phase.tick.color, Style.phase.tick.stroke),
								StrokeShape(shape, Style.phase.beat.color, Style.phase.beat.stroke)
							)
						}
				
				bar.toSeq ++ lines
			}
	
	//------------------------------------------------------------------------------
	//## wiring
	
	figures observe canvas.figures.set
	
	//------------------------------------------------------------------------------
	//## output
	
	val mouseWheel:Events[Int]	= 
			canvas.mouse.wheelRotation
	
	private val leftJump:Events[Double]	= 
			canvas.mouse.leftPress							orElse 
			canvas.mouse.leftDrag							map 
			{ _.getX }										snapshot
			value2gui										map 
			{ case (x, value2gui) => value2gui inverse x }	map
			clampMiniMax
	
	private val middleReset:Events[Double]	= 
			canvas.mouse.rightPress tag 0.0
	
	val jump:Events[Double]	=
			leftJump orElse middleReset
}
