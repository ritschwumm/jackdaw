package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Vorbiscomment extends Inspector {
	def name	= "vorbiscomment"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				_		<- MediaUtil requireCommand "vorbiscomment"
				result	<-
						MediaUtil runCommand (
							"vorbiscomment",
							input.getPath
						)
			}
			yield {
				val extract	= MediaUtil extractFrom result.out
				Metadata(
					title	= extract("""TITLE=(.*)""".r),
					artist	= extract("""ARTIST=(.*)""".r),
					album	= extract("""ALBUM=(.*)""".r)
					// genre	= extract("""GENRE=(.*)""".r)
				)
			}
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".ogg")
}
