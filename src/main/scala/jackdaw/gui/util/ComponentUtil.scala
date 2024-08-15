package jackdaw.gui.util

import javax.swing.JComponent

import scutil.core.implicits.*
import scutil.lang.*
import scutil.geom.*
import scutil.gui.implicits.*
import scutil.gui.CasterInstances.*
import scutil.gui.ComponentUnderMouse
import scutil.gui.geom.*
import scutil.gui.geom.extensions.*
import scutil.log.*

import screact.*
import screact.swing.*

/** swing component utility functions */
object ComponentUtil extends Logging {
	private val componentUnderMouse	=
		new ComponentUnderMouse(
			100.duration.millis,
			(message,exception)	=> ERROR(message, exception)
		)

	def innerIntRectSignal(component:JComponent):Signal[IntRect]	=
		SwingWidget.signal(
			(component:ComponentCaster).connect,
			thunk { component.innerRectangle.toIntRect }
		)

	def innerSgRectangleSignal(component:JComponent):Signal[SgRectangle]	=
		SwingWidget.signal(
			(component:ComponentCaster).connect,
			thunk { component.innerRectangle.toSgRectangle }
		)

	def underMouseSignal(component:JComponent):Signal[Boolean]	= {
		val out	= cell(component.underMousePointer)
		val set	= out.set(_)
		// NOTE ugly backref to keep ComponentUnderMouse from dropping the connection
		component.putClientProperty("UNDER_MOUSE", set)
		componentUnderMouse.listen(component, set)
		out.signal
	}
}
