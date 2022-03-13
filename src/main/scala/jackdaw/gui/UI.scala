package jackdaw.gui

import scala.language.implicitConversions

import javax.swing.*

object UI {
	implicit def JComponent_is_UI(component:JComponent):UI	= new TrivialUI(component)
}

/** to be implemented by all UI components */
trait UI {
	def component:JComponent
}

private final class TrivialUI(val component:JComponent) extends UI
