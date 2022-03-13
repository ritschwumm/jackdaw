package jackdaw.range

import scutil.math.functions.*

object FilterRange {
	val min		= -1.0
	val neutral	= 0.0
	val max		= +1.0
	val size	= max - min

	def clamp(it:Double):Double	=
			clampDouble(
				it,
				min,
				max
			)
}
