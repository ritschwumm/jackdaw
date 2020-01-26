package jackdaw.gui.util

import java.awt.Component
import java.awt.event._

import scutil.base.implicits._
import scutil.gui.CasterInstances._
import scutil.gui.InputEventPredicates._

import screact._
import screact.swing._

final class Mouse(component:Component) {
	val events	=
		(SwingWidget	events	(component:MouseCaster).connect)		orElse
		(SwingWidget	events	(component:MouseMotionCaster).connect)	orElse
		(SwingWidget	events	(component:MouseWheelCaster).connect)

	val leftPress	= events filter (mousePressed	&& button1)
	val leftRelease	= events filter (mouseReleased	&& button1)
	val leftDrag	= events filter (mouseDragged	&& buttonDown1)

	val rightPress	= events filter (mousePressed	&& button3)

	val wheel		= events filter mouseWheeled

	val wheelRotation:Events[Int]	=
		wheel collect {
			case ev:MouseWheelEvent => -ev.getWheelRotation
		}
}
