package jackdaw.key

import scala.math.*

import scutil.math.functions.*

final case class MusicChord(root:MusicPitch, scale:MusicScale) {
	def detuned(semitoneOffset:Double):DetunedChord	= {
		val shift:Int		= round(semitoneOffset).toInt
		// val small:Int	= round(moduloDouble(semitoneOffset - 0.5, 1) * 4 - 2).toInt
		val detune:Detune	=
			moduloDouble(semitoneOffset - 0.5, 1) match {
				case x if x < 0.2	=> Detune.VeryLow
				case x if x < 0.4	=> Detune.Low
				case x if x > 0.8	=> Detune.VeryHigh
				case x if x > 0.6	=> Detune.High
				case _				=> Detune.InTune
			}
		DetunedChord(
			MusicChord(
				root.move(shift),
				scale
			),
			detune
		)
	}
}
