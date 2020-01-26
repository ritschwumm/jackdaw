package jackdaw.key

import scutil.lang._

object MusicKey {
	object P {
		import jackdaw.key.{ MusicKey => Self }

		val Silence		= Prism.Gen[Self,Self.Silence.type]
		val Chord		= Prism.Gen[Self,Self.Chord]
	}

	case object Silence							extends MusicKey
	final case class Chord(value:MusicChord)	extends MusicKey
}

sealed trait MusicKey {
	def toMusicChordOption:Option[MusicChord]	=
		this match {
			case MusicKey.Silence		=> None
			case MusicKey.Chord(value)	=> Some(value)
		}
}
