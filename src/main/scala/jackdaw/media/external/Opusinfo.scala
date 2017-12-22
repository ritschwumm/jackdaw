package jackdaw.media

import java.io.File

import scutil.core.implicits._

import jackdaw.util.Checked

object Opusinfo extends Inspector {
	def name	= "opusinfo"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				_		<- MediaUtil requireCommand "opusinfo"
				result	<-
						MediaUtil runCommand (
							"opusinfo",
							input.getPath
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
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".opus")
}
