package djane.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

object GridBagUI {
	def apply(subs:GridBagItem*):GridBagUI	= new GridBagUI(subs)
}

final class GridBagUI(subs:Seq[GridBagItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new GridBagLayout
	
	subs foreach { case GridBagItem(ui,constraint) => 
		panel add (ui.component, constraint) 
	}
	
	val component:JComponent	= panel
}
