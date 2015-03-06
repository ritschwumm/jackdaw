package jackdaw.key

sealed trait MusicScale {
	def cata[T](major: =>T, minor: =>T):T	=
			this match {
				case Major	=> major
				case Minor	=> minor
			}
}

case object Major	extends MusicScale
case object Minor	extends MusicScale
