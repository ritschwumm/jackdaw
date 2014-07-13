package djane.gui.shape

import java.awt.Shape
import java.awt.geom._

import scutil.implicits._

import scgeom._

case class Draft(close:Boolean, points:Seq[SgPoint]) {
	def toShape:Shape	= 
			new Path2D.Double doto appendTo
	
	def appendTo(out:Path2D.Double) {
		points.zipWithIndex foreach {
			case (SgPoint(x,y), 0)	=> out moveTo (x,y)
			case (SgPoint(x,y), _)	=> out lineTo (x,y)
		} 
		if (close) {
			out.closePath()
		}
	}
}
