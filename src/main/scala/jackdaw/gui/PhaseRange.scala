package jackdaw.gui

import scgeom._

import jackdaw.gui.util._

object PhaseRange {
	val span	= SgSpan startEnd (-0.5, +0.5)

	// NOTE hack to show inverse a little bit more often
	private val epsilon	= -0.0001

	val inside	= GeomUtil containsInclusive (span, epsilon)

	def clamp(raw:Double):Double	=
			GeomUtil clampValue (span, raw)
}
