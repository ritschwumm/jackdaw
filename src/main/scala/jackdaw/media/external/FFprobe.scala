package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object FFprobe extends Inspector {
	def name	= "ffprobe"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			// no suffix check
			_		<-	MediaUtil requireCommand "ffprobe"
			result	<-	MediaUtil.runCommand(
							"ffprobe",	input.toString
							/*
							// this no longer works (?)
							"ffprobe",	"-y",
							"-i",		input.toString,
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
