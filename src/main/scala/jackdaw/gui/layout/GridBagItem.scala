package jackdaw.gui

import java.awt.GridBagConstraints
import java.awt.Insets

import scutil.gui.gridbag.*

final case class GridBagItem(ui:UI, gbc:GridBagConstraints) {
	def pos(x:GridBag.Position, y:GridBag.Position):GridBagItem	= constrain(_.pos(x,y))
	def size(x:GridBag.Size, y:GridBag.Size):GridBagItem		= constrain(_.size(x,y))

	def weight(x:Double, y:Double):GridBagItem	= constrain(_.weight(x,y))
	def ipad(x:Int, y:Int):GridBagItem			= constrain(_.ipad(x,y))

	def anchor(v:GridBag.Anchor):GridBagItem	= constrain(_.anchor(v))
	def fill(v:GridBag.Fill):GridBagItem		= constrain(_.fill(v))

	def insets(v:Insets):GridBagItem										= constrain(_.insets(v))
	def insetsTLBR(top:Int, left:Int, bottom:Int, right:Int):GridBagItem	= constrain(_.insetsTLBR(top,left,bottom,right))

	private def constrain(func:GridBagConstraints=>GridBagConstraints):GridBagItem	= GridBagItem(ui, func(gbc))
}
