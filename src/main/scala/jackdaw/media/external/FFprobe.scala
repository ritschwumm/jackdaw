package jackdaw.media

import java.io.File

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object FFprobe extends Inspector {
	def name	= "ffprobe"

	def readMetadata(input:File):Checked[Metadata] =
		for {
			// no suffix check
			_		<-	MediaUtil requireCommand "ffprobe"
			result	<-	MediaUtil.runCommand(
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
				title	= extract(re"""    title\s*: (.*)"""),
				artist	= extract(re"""    artist\s*: (.*)"""),
				album	= extract(re"""    album\s*: (.*)""")
				// genre	= extract(re"""    genre\s*: (.*)""")
			)
		}
}
