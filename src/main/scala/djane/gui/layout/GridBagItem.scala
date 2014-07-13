package djane.gui

import java.awt.GridBagConstraints
import java.awt.Insets

import scutil.lang._
import scutil.gui.GridBagDSL._

object GridBagItem {
	implicit def UI_is_GridBagItem[T<%UI](ui:T):GridBagItem	= GridBagItem(ui, GBC)
} 

case class GridBagItem(ui:UI, gbc:GridBagConstraints) {
	def pos(x:GridBagConstraintsPosition, y:GridBagConstraintsPosition):GridBagItem	= constrain(_ pos			(x,y))
		
	def size(x:GridBagConstraintsSize, y:GridBagConstraintsSize):GridBagItem		= constrain(_ size			(x,y))
	def weight(x:Double, y:Double):GridBagItem										= constrain(_ weight		(x,y))
	def ipad(x:Int, y:Int):GridBagItem												= constrain(_ ipad			(x,y))
	
	def anchor(v:GridBagConstraintsAnchor):GridBagItem								= constrain(_ anchor		v)
	def fill(v:GridBagConstraintsFill):GridBagItem									= constrain(_ fill			v)
	
	def insets(v:Insets):GridBagItem												= constrain(_ insets		v)
	def insetsTLBR(top:Int, left:Int, bottom:Int, right:Int):GridBagItem			= constrain(_ insetsTLBR	(top,left,bottom,right))
	
	private def constrain(func:Endo[GridBagConstraints]):GridBagItem				= copy(ui, func(gbc))
}
