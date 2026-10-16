package jackdaw.gui.shape

import java.awt.Shape
import java.awt.geom.*

import scutil.core.implicits.*
import scutil.gui.geom.*

object Poly {
	def of(drafts:Draft*):Poly	= Poly(drafts.toVector)

	def toShape(poly:Poly):Shape	=
		(new Path2D.Double).doto(polyAppend(poly))

	//------------------------------------------------------------------------------

	private def polyAppend(poly:Poly)(out:Path2D.Double):Unit	= {
		poly.drafts.foreach(draftAppend(out))
	}

	private def draftAppend(out:Path2D.Double)(draft:Draft):Unit	= {
		val closed	= draft.points.head == draft.points.last
		val last	= draft.points.size - 1
		draft.points.zipWithIndex.foreach {
			case (SgPoint(x,y), 0)								=> out.moveTo(x,y)
			case (SgPoint(x,y), i)	if !(closed && i == last)	=> out.lineTo(x,y)
			case _												=>
		}
		if closed then {
			out.closePath()
		}
	}
}

final case class Poly(drafts:Vector[Draft]) {
	def toShape:Shape	= Poly.toShape(this)
}
