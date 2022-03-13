package jackdaw.key

import scutil.lang.*

object MusicKey {
	object P {
		import jackdaw.key.{ MusicKey as Self }

		val Silence		= Prism.subType[Self,Self.Silence.type]
		val Chord		= Prism.subType[Self,Self.Chord]
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
