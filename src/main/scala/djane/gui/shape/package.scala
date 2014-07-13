package djane.gui

import scgeom._

package object shape {
	def poly(drafts:Draft*)				= Poly(drafts)
	def open(points:(Double,Double)*)	= Draft(false,	points map SgPoint.fromPair)
	def closed(points:(Double,Double)*)	= Draft(true,	points map SgPoint.fromPair)
}
