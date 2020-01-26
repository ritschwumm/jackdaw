package jackdaw.key

object MusicScale {
	case object Major	extends MusicScale
	case object Minor	extends MusicScale
}

sealed trait MusicScale {
	def cata[T](major: =>T, minor: =>T):T	=
		this match {
			case MusicScale.Major	=> major
			case MusicScale.Minor	=> minor
		}
}
