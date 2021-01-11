package jackdaw.media

import java.io.File

import scutil.jdk.implicits._

import jackdaw.util.Checked

object Metaflac extends Inspector {
	def name	= "metaflac"

	def readMetadata(input:File):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil requireCommand "metaflac"
			result	<-	MediaUtil.runCommand(
							"metaflac",
							"--list",
							input.getPath
						)
		}
		yield {
			val extract	= MediaUtil extractFrom result.out
			Metadata(
				title	= extract(re""".*TITLE=(.*)"""),
				artist	= extract(re""".*ARTIST=(.*)"""),
				album	= extract(re""".*ALBUM=(.*)""")
				// genre	= extract(re""".*GENRE=(.*)""")
			)
		}

	private val recognizeFile:File=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".flac", ".flc")
}
