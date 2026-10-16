package jackdaw.key

import scutil.lang.*

object MusicKey {
	val P	= Prism.Gen[MusicKey]
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
