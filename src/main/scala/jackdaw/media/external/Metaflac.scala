package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object Metaflac extends Inspector {
	def name	= "metaflac"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil requireCommand "metaflac"
			result	<-	MediaUtil.runCommand(
							"metaflac",
							"--list",
							input.toString
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

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".flac", ".flc")
}
