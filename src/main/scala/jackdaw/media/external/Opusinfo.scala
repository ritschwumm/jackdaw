package jackdaw.media

import java.io.File

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
					title	= extract("""\ttitle=(.*)""".r),
					artist	= extract("""\tartist=(.*)""".r),
					album	= extract("""\talbum=(.*)""".r)
					// genre	= extract("""\tgenre=(.*)""".r)
				)
			}
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".opus")
}
