package jackdaw.gui.util

import java.awt.Component
import java.awt.event.*

import scutil.core.implicits.*
import scutil.gui.AllCasters.*
import scutil.gui.InputEventPredicates.*

import screact.*
import screact.swing.*

final class Mouse(component:Component) {
	val events	=
		SwingWidget.events(component.onMouseEvent)			`orElse`
		SwingWidget.events(component.onMouseMotionEvent)	`orElse`
		SwingWidget.events(component.onMouseWheelMoved)

	val leftPress	= events.filter(mousePressed	&& button1)
	val leftRelease	= events.filter(mouseReleased	&& button1)
	val leftDrag	= events.filter(mouseDragged	&& buttonDown1)

	val rightPress	= events.filter(mousePressed	&& button3)

	val wheel		= events.filter(mouseWheeled)

	val wheelRotation:Events[Int]	=
		wheel.collect {
			case ev:MouseWheelEvent => -ev.getWheelRotation
		}
}
