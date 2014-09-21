package jackdaw.gui.util

import java.awt.Component
import java.awt.event._

import scutil.implicits._
import scutil.gui.CasterInstances._
import scutil.gui.InputEventPredicates._

import screact._
import screact.swing._

final class Mouse(component:Component) {
	lazy val events	= 
			(SwingWidget	events	(component:MouseCaster).connect)		orElse
			(SwingWidget	events	(component:MouseMotionCaster).connect)	orElse
			(SwingWidget	events	(component:MouseWheelCaster).connect)	
			
	lazy val leftPress		= events filter (mousePressed	&& button1)
	lazy val leftRelease	= events filter (mouseReleased	&& button1)
	lazy val leftDrag		= events filter (mouseDragged	&& buttonDown1)
	
	lazy val rightPress		= events filter (mousePressed	&& button3)
	
	lazy val wheel			= events filter mouseWheeled

	lazy val wheelRotation:Events[Int]	=
			wheel collect {
				case ev:MouseWheelEvent => -ev.getWheelRotation
			}
}
