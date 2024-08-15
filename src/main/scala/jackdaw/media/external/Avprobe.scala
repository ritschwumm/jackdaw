package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*

import jackdaw.util.Checked

object Avprobe extends Inspector {
	def name	= "avprobe"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			// no suffix check
			_		<- MediaUtil.requireCommand("avprobe")
			result	<-
					MediaUtil.runCommand(
						"avprobe",	input.toString
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
			val extract	= MediaUtil.extractFrom(result.err)
			Metadata(
				title	= extract(re"""    title\s*: (.*)"""),
				artist	= extract(re"""    artist\s*: (.*)"""),
				album	= extract(re"""    album\s*: (.*)""")
				// genre	= extract(re"""    genre\s*: (.*)""")
			)
		}
}
