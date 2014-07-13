package djane.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

object HBoxUI {
	def apply(subs:BoxItem*):HBoxUI	=
			new HBoxUI(subs)
}

final class HBoxUI(subs:Seq[BoxItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BoxLayout(panel, BoxLayout.X_AXIS)
	
	private val items	=
			subs map { 
				case BoxComponent(ui)	=> ui.component
				case BoxGlue			=> Box.createHorizontalGlue()
				case BoxStrut(size)		=> Box createHorizontalStrut size
			} 
	items foreach panel.add
	
	val component:JComponent	= panel
}
