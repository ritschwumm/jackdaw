package jackdaw.gui

import javax.swing.*

object VBoxUI {
	def apply(subs:BoxItem*):VBoxUI	=
		new VBoxUI(subs.toVector)
}

final class VBoxUI(subs:Seq[BoxItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BoxLayout(panel, BoxLayout.Y_AXIS)

	private val items	=
		subs map {
			case BoxItem.Component(ui)	=> ui.component
			case BoxItem.Glue			=> Box.createVerticalGlue()
			case BoxItem.Strut(size)	=> Box createVerticalStrut size
		}
	items foreach panel.add

	val component:JComponent	= panel
	component.putClientProperty("STRONG_REF", this)
}
