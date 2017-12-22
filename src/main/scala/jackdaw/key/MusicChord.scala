package jackdaw.key

import scala.math._

import scutil.math.functions._

final case class MusicChord(root:MusicPitch, scale:MusicScale) {
	def detuned(semitoneOffset:Double):DetunedChord	= {
		val shift:Int		= round(semitoneOffset).toInt
		// val small:Int	= round(moduloDouble(semitoneOffset - 0.5, 1) * 4 - 2).toInt
		val detune:Detune	=
				moduloDouble(semitoneOffset - 0.5, 1) match {
					case x if x < 0.2	=> VeryLow
					case x if x < 0.4	=> Low
					case x if x > 0.8	=> VeryHigh
					case x if x > 0.6	=> High
					case _				=> InTune
				}
		DetunedChord(
			MusicChord(
				root move shift,
				scale
			),
			detune
		)
	}
}
