package jackdaw.audio

import scutil.math._

import jackdaw.audio.PitchMath._

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
