package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

/** used to break cycles between components */
final class DelayUI(child: =>UI) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BorderLayout
	val component:JComponent	= panel
	
	def init() {
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
