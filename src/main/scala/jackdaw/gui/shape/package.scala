package jackdaw.gui

import scgeom._

package object shape {
	def poly(drafts:Draft*)				= Poly(drafts.toVector)
	def open(points:(Double,Double)*)	= Draft(false,	points.toVector map SgPoint.fromPair)
	def closed(points:(Double,Double)*)	= Draft(true,	points.toVector map SgPoint.fromPair)
}
