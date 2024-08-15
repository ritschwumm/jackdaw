package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object Vorbiscomment extends Inspector {
	def name	= "vorbiscomment"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil.requireCommand("vorbiscomment")
			result	<-	MediaUtil.runCommand(
							"vorbiscomment",
							input.toString
						)
		}
		yield {
			val extract	= MediaUtil.extractFrom(result.out)
			Metadata(
				title	= extract(re"""TITLE=(.*)"""),
				artist	= extract(re"""ARTIST=(.*)"""),
				album	= extract(re"""ALBUM=(.*)""")
				// genre	= extract(re"""GENRE=(.*)""")
			)
		}

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".ogg")
}
