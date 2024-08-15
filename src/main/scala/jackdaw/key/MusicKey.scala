package jackdaw.key

import scutil.lang.*

object MusicKey {
	object P {
		val Silence		= Prism.subType[MusicKey,MusicKey.Silence.type]
		val Chord		= Prism.subType[MusicKey,MusicKey.Chord]
	}
}

enum MusicKey {
	case Silence
	case Chord(value:MusicChord)

	def toMusicChordOption:Option[MusicChord]	=
		this match {
			case Silence		=> None
			case Chord(value)	=> Some(value)
		}
}
