package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object Opusinfo extends Inspector {
	def name	= "opusinfo"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil requireCommand "opusinfo"
			result	<-	MediaUtil.runCommand(
							"opusinfo",
							input.toString
						)
		}
		yield {
			val extract	= MediaUtil extractFrom result.out
			Metadata(
				title	= extract(re"""\ttitle=(.*)"""),
				artist	= extract(re"""\tartist=(.*)"""),
				album	= extract(re"""\talbum=(.*)""")
				// genre	= extract(re"""\tgenre=(.*)""")
			)
		}

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".opus")
}
