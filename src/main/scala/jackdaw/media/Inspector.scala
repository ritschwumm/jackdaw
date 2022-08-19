package jackdaw.media

import java.nio.file.Path

import jackdaw.util.Checked

object Inspector {
	val all:Seq[Inspector]	=
		Vector(
			Madplay,
			Mpg123,
			Faad,
			Vorbiscomment,
			Metaflac,
			Opusinfo,
			FFprobe,
			Avprobe,
			Mp3agic,
			JOgg
		)

	def readMetadata(input:Path):Option[Metadata]	=
		MediaUtil
		.worker[Inspector,Metadata](
			all,
			_.name,
			_ readMetadata input
		)
}

/** interface to a decoder */
trait Inspector {
	def name:String

	/** read metadata */
	def readMetadata(input:Path):Checked[Metadata]
}
