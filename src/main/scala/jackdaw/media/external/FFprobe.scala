package jackdaw.media

import java.io.File

import jackdaw.util.Checked

object FFprobe extends Inspector {
	def name	= "ffprobe"
				
	def readMetadata(input:File):Checked[Metadata] =
			for {
				// no suffix check
				_		<- MediaUtil requireCommand "ffprobe"
				result	<-
						MediaUtil runCommand (
							"ffprobe",	input.getPath
							/*
							// this no longer works (?)
							"ffprobe",	"-y",		
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
