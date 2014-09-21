package jackdaw.gui.shape

import java.awt.Shape
import java.awt.geom._

import scutil.lang.ISeq
import scutil.implicits._

case class Poly(drafts:ISeq[Draft]) {
	def toShape:Shape	= 
			new Path2D.Double doto appendTo
	
	def appendTo(out:Path2D.Double) {
		drafts foreach { _ appendTo out }
	}
}
