package jackdaw.gui.util

import javax.swing.JComponent

import scutil.lang._
import scutil.implicits._
import scutil.geom._
import scutil.gui.implicits._
import scutil.gui.CasterInstances._
import scutil.gui.ComponentUnderMouse
import scutil.log._

import scgeom._

import screact._
import screact.swing._

/** swing component utility functions */
object ComponentUtil extends Logging {
	private val componentUnderMouse	= new ComponentUnderMouse({
		(message,exception)	=> ERROR(message, exception)
	})
	
	def innerIntRectSignal(component:JComponent):Signal[IntRect]	=
			SwingWidget signal (
					(component:ComponentCaster).connect,
					thunk { component.innerRectangle.toIntRect })
					
	def innerSgRectangleSignal(component:JComponent):Signal[SgRectangle]	=
			SwingWidget signal (
					(component:ComponentCaster).connect,
					thunk { component.innerRectangle.toSgRectangle })
	
	def underMouseSignal(component:JComponent):Signal[Boolean]	= {
		val out	= cell(component.underMousePointer)
		val set	= out.set _
		// NOTE ugly backref to keep ComponentUnderMouse from dropping the connection
		component putClientProperty ("UNDER_MOUSE", set)
		componentUnderMouse listen (component, set)
		out
	}
}
