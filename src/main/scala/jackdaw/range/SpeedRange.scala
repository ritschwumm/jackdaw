package jackdaw.range

import scutil.math.functions.*

import jackdaw.range.PitchMath.*

object SpeedRange {
	val	min	= bpm(30.0)
	val	max	= bpm(240.0)

	def clamp(it:Double):Double	=
		clampDouble(
			it,
			min,
			max
		)
}
