package jackdaw.key

enum MusicScale {
	case Major
	case Minor

	def cata[T](major: =>T, minor: =>T):T	=
		this match {
			case Major	=> major
			case Minor	=> minor
		}
}
