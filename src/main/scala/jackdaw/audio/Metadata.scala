package jackdaw.audio

/** audio file metadata */
case class Metadata(
	title:Option[String], 
	artist:Option[String], 
	album:Option[String],
	genre:Option[String]
)
