package jackdaw.gui

import javax.swing._

import scutil.lang.ISeq

object HBoxUI {
	def apply(subs:BoxItem*):HBoxUI	=
			new HBoxUI(subs.toVector)
}

final class HBoxUI(subs:ISeq[BoxItem]) extends UI {
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
