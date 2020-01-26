package jackdaw.range

import scaudio.math._

object PitchMath {
	def cents(it:Double):Double	= octave2frequency(it/1200.0)

	/** convert bpm into bps (beats per second) */
	def bpm(it:Double):Double	= it / secondsPerMinute
}
