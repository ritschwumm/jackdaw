package jackdaw.gui

import java.awt.Shape
import java.awt.geom._

import scutil.lang.Nes
import scutil.implicits._

import scgeom._

package object shape {
	def poly(drafts:Draft*)						= Poly(drafts.toVector)
	def draft(point:SgPoint, points:SgPoint*)	= Draft(Nes(point, points.toVector))
	
	//------------------------------------------------------------------------------
	
	def polyShape(poly:Poly):Shape	=
			new Path2D.Double doto polyAppend(poly)
			
	//------------------------------------------------------------------------------
	
	private def polyAppend(poly:Poly)(out:Path2D.Double) {
		poly.drafts foreach draftAppend(out)
	}
	
	private def draftAppend(out:Path2D.Double)(draft:Draft) {
		draft.points.zipWithIndex foreach {
			case (SgPoint(x,y), 0)	=> out moveTo (x,y)
			case (SgPoint(x,y), _)	=> out lineTo (x,y)
		} 
		if (draft.points.head == draft.points.tail) {
			out.closePath()
		}
	}
}
