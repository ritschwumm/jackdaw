package jackdaw.media

import java.io.File

import scutil.lang._

import jackdaw.util.Checked

object Inspector {
	val all:ISeq[Inspector]	=
			Vector(
				Madplay,
				Mpg123,
				Faad,
				Vorbiscomment,
				Metaflac,
				Opusinfo,
				Avprobe,
				Mp3agic,
				JOgg
			)
	
	def readMetadata(input:File):Option[Metadata]	=
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
	def readMetadata(input:File):Checked[Metadata]
}
