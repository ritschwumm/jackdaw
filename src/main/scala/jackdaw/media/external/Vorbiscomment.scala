package jackdaw.media

import java.io.File

import scutil.core.implicits._

import jackdaw.util.Checked

object Vorbiscomment extends Inspector {
	def name	= "vorbiscomment"

	def readMetadata(input:File):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil requireCommand "vorbiscomment"
			result	<-	MediaUtil runCommand (
							"vorbiscomment",
							input.getPath
						)
		}
		yield {
			val extract	= MediaUtil extractFrom result.out
			Metadata(
				title	= extract(re"""TITLE=(.*)"""),
				artist	= extract(re"""ARTIST=(.*)"""),
				album	= extract(re"""ALBUM=(.*)""")
				// genre	= extract(re"""GENRE=(.*)""")
			)
		}

	private val recognizeFile:File=>Checked[Unit]	=
		MediaUtil requireFileSuffixIn (".ogg")
}
