package jackdaw.gui

import java.awt.{ List as _, Canvas as _, * }
import javax.swing.*

import scutil.gui.implicits.*

import screact.*
import sc2d.*

import jackdaw.gui.util.*

final class ButtonUI(size:Dimension, style:Signal[ButtonStyle], enabled:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components

	private val canvas	= new CanvasWrapper(None, Hints.highQuality)

	val component:JComponent	= canvas.component
	component.putClientProperty("STRONG_REF", this)
	component	setAllSizes	size

	//------------------------------------------------------------------------------
	//## wiring

	private val down	=
		(canvas.mouse.leftPress		tag true)	orElse
		(canvas.mouse.leftRelease	tag false)	hold
		false

	private val hovered	= ComponentUtil underMouseSignal component
	private val armed	= signal { down.current && hovered.current && enabled.current }
	private val gate	= armed delay false

	private val figures	=
		signal {
			val	styleCur	= style.current
				 if (!enabled.current)	styleCur.disabled
			else if (armed.current)		styleCur.pressed
			else if (hovered.current)	styleCur.hovered
			else						styleCur.inactive
		}

	figures observeNow canvas.figures.set

	//------------------------------------------------------------------------------
	//## output

	val pressed:Signal[Boolean]	=
		armed

	val actions:Events[Unit]	=
		canvas.mouse.leftRelease gate gate tag (())
}
