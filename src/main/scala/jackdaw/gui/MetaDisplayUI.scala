package jackdaw.gui

import javax.swing._

import scutil.base.implicits._

import screact._

final class MetaDisplayUI(value:Signal[String], strong:Boolean, start:Boolean) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components

	private val label	= new JLabel
	label setFont					(strong	cata (Style.meta.display.weak.font,		Style.meta.display.strong.font))
	label setForeground 			(strong	cata (Style.meta.display.weak.color,	Style.meta.display.strong.color))
	label setHorizontalAlignment	(start	cata (SwingConstants.RIGHT,				SwingConstants.LEFT))
	val component:JComponent	= label

	//------------------------------------------------------------------------------
	//## wiring

	value observeNow label.setText
}
