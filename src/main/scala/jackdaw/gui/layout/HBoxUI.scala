package jackdaw.gui

import javax.swing._

object HBoxUI {
	def apply(subs:BoxItem*):HBoxUI	=
		new HBoxUI(subs.toVector)
}

final class HBoxUI(subs:Seq[BoxItem]) extends UI {
	private val	panel	= new JPanel
	panel setLayout new BoxLayout(panel, BoxLayout.X_AXIS)

	private val items	=
		subs map {
			case BoxItem.Component(ui)	=> ui.component
			case BoxItem.Glue			=> Box.createHorizontalGlue()
			case BoxItem.Strut(size)	=> Box createHorizontalStrut size
		}
	items foreach panel.add

	val component:JComponent	= panel
}
