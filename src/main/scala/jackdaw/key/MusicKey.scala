package jackdaw.key

sealed trait MusicKey {
	def toMusicChordOption:Option[MusicChord]	=
			this match {
				case Silence		=> None
				case Chord(value)	=> Some(value)
			}
}

case object Silence					extends MusicKey
final case class Chord(value:MusicChord)	extends MusicKey
