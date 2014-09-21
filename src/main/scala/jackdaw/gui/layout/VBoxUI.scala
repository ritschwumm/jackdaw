package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

import scutil.lang.ISeq

object VBoxUI {
	def apply(subs:BoxItem*):VBoxUI	=
			new VBoxUI(subs.toVector)
}

final class VBoxUI(subs:ISeq[BoxItem]) extends UI {
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
