package jackdaw.audio

import scutil.math._

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
