package djane.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

object VBoxUI {
	def apply(subs:BoxItem*):VBoxUI	=
			new VBoxUI(subs)
}

final class VBoxUI(subs:Seq[BoxItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BoxLayout(panel, BoxLayout.Y_AXIS)
	
	private val items	=
			subs map { 
				case BoxComponent(ui)	=> ui.component
				case BoxGlue			=> Box.createVerticalGlue()
				case BoxStrut(size)		=> Box createVerticalStrut size
			}
	items foreach panel.add
	
	val component:JComponent	= panel
}
