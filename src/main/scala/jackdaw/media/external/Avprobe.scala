package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Avprobe extends Inspector {
	def name	= "avprobe"
				
	def readMetadata(input:File):Checked[Metadata] =
			for {
				// no suffix check
				_		<- MediaUtil requireCommand "avprobe"
				result	<-
						MediaUtil runCommand (
							"avprobe",	input.getPath
							/*
							// this no longer works
							"avconv",	"-y",		
							"-i",		input.getPath,
							"-vn",		"-an",
							"-null",	"-"
							*/
						)
			}
			yield {
				val extract	= MediaUtil extractFrom result.err
				Metadata(
					title	= extract("""    title\s*: (.*)""".r),
					artist	= extract("""    artist\s*: (.*)""".r),
					album	= extract("""    album\s*: (.*)""".r)
					// genre	= extract("""    genre\s*: (.*)""".r)
				)
			}
}
