package jackdaw.audio

import scutil.math._

object TrimRange {
	val min		= 0.0
	val neutral	= 1.0
	val max		= 2.0
	
	val size	= max - min
	
	def clamp(it:Double):Double	=
			clampDouble(
				it,
				min,
				max
			)
}
