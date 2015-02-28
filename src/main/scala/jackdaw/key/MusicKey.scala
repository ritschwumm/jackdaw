package jackdaw.key

sealed trait MusicKey
case object Silence									extends MusicKey
case class Chord(root:MusicPitch, scale:MusicScale)	extends MusicKey
