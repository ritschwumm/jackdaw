package jackdaw.key

object MusicKey {
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
