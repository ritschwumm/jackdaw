package jackdaw.gui

import javax.swing.*

import scutil.gui.gridbag.*

extension(component:JComponent) {
	def asUi:UI	= new TrivialUI(component)
}

extension(ui:UI) {
	def gbi	= GridBagItem(ui, GBC)
}
