package jackdaw.range

import scutil.math.functions._

object PitchRange {
	val min		= -0.5
	val neutral	= 0.0
	val max		= +0.5

	def clamp(it:Double):Double	=
		clampDouble(
			it,
			min,
			max
		)
}
