package jackdaw.gui

import java.awt.{ List=>_, _ }
import javax.swing._

import scutil.lang.ISeq

object GridBagUI {
	def apply(subs:GridBagItem*):GridBagUI	=
			new GridBagUI(subs.toVector)
}

final class GridBagUI(subs:ISeq[GridBagItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new GridBagLayout
	
	subs foreach { case GridBagItem(ui,constraint) =>
		panel add (ui.component, constraint)
	}
	
	val component:JComponent	= panel
}
