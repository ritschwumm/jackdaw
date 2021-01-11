package jackdaw.gui

import java.awt.{ List => _, _ }
import javax.swing._

import screact._

final class SwitchUI(child:Signal[UI]) extends UI with Observing {
	// becomes too wide in a HBoxUI without this
	private val panel	=
		new JPanel {
			override def getMaximumSize:Dimension	= getPreferredSize
		}
	panel setLayout new BorderLayout

	child observeNow { ui =>
		panel.removeAll()
		panel.add(ui.component, BorderLayout.CENTER)
		panel.invalidate()
		panel.revalidate()
		panel.repaint()
	}

	val component:JComponent	= panel
}
