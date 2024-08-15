package jackdaw.gui

import javax.swing.*

/** to be implemented by all UI components */
trait UI {
	def component:JComponent
}

private final class TrivialUI(val component:JComponent) extends UI
