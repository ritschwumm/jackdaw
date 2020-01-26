package jackdaw.gui

import java.awt.{ List=>_, _ }
import javax.swing._

/** used to break cycles between UI components */
final class DelayUI(child: =>UI) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BorderLayout
	val component:JComponent	= panel

	/** has to be called after the component has been added to the parent */
	def init():Unit	= {
		// avoid GC for the child UI as long as the Component is still alive
		val real	= child
		val realC	= real.component
		realC putClientProperty ("SELF", real)

		panel.removeAll()
		panel add (realC, BorderLayout.CENTER)
		panel.invalidate()
		panel.revalidate()
		panel.repaint()
	}
}
