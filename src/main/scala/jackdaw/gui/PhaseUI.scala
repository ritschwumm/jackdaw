package jackdaw.gui

import javax.swing.*

import scala.math.*

import scutil.core.implicits.*
import scutil.gui.geom.*
import scutil.gui.geom.extensions.*

import screact.*
import sc2d.*

import jackdaw.data.*
import jackdaw.gui.util.*

object PhaseUI {
	private val subDivide	= Schema.default.beatsPerMeasure

	// TODO hardcoded insets
	private val lineInsets	= SgSpanInsets.startEnd(2, 3)
}

/** values are linear [-.5..+.5] */
final class PhaseUI(value:Signal[Option[Double]], rhythm:Signal[Option[Rhythm]]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components

	private val canvas	= new CanvasWrapper(Some(Style.phase.background.color), Hints.lowQuality)

	val component:JComponent	= canvas.component
	component.putClientProperty("STRONG_REF", this)
	component setBorder	Style.phase.border

	//------------------------------------------------------------------------------
	//## input

	private val value2gui	=
		canvas.bounds map { it =>
			SgLinearTransform1D.fromTo(PhaseRange.span, it.x)
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

			val lineSpan		= trackBoundsCur.y inset PhaseUI.lineInsets

			val bar	=
				for {
					value	<- valueCur
					rect	=
							// extreme values fill complete area
							if (PhaseRange inside value) {
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
					subRaster	= rhythm.schema.beatsPerMeasure * PhaseUI.subDivide
					max			= (subRaster - 1) / 2
					index		<- -max to +max
				}
				yield {
					val shape	= SgLine.vertical(value2guiCur(index.toDouble / subRaster), lineSpan).toAwtLine2D
					((index % PhaseUI.subDivide) == 0).cata(
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
		PhaseRange.clamp

	private val middleReset:Events[Double]	=
		canvas.mouse.rightPress tag 0.0

	val jump:Events[Double]	=
		leftJump orElse middleReset
}
