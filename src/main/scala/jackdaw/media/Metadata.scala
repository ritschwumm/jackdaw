package jackdaw.media

/** audio file metadata */
final case class Metadata(
	title:Option[String],
	artist:Option[String],
	album:Option[String]
	// genre:Option[String]
)
