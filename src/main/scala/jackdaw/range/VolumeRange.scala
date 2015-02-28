package jackdaw.range

import scutil.math._

object VolumeRange {
	val max		= 1.0
	val min		= 0.0
	val size	= max - min
	
	val alot	= max * 2 / 3
	
	def clamp(it:Double):Double	=
			clampDouble(
				it,
				min,
				max
			)
}
