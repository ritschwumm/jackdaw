package jackdaw.media

import java.nio.file.*

import scutil.jdk.implicits.*
import scutil.lang.implicits.*
import scutil.math.functions.*

import jackdaw.util.Checked

object Madplay extends Inspector with Decoder {
	def name	= "madplay"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil.requireCommand("madplay")
			result	<-	MediaUtil.runCommand(
							"madplay",
							"-T",
							input.toString
						)
		}
		yield {
			val extract	= MediaUtil.extractFrom(result.err)
			Metadata(
				title	= extract(re"""\s*Title: (.*)"""),
				artist	= extract(re"""\s*Artist: (.*)"""),
				album	= extract(re"""\s*Album: (.*)""")
				// genre		= extract(re"""\s*Genre: (.*)""")
				// publisher	= extract(re"""\s*Publisher: (.*)"""),
				// comment		= extract(re"""\s*Comment: (.*)"""),
			)
		}

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil.requireCommand("madplay")
			res	<-	MediaUtil.runCommand(
						"madplay",
						"--output",			"wav:" + output.toString,
						"--bit-depth",		"16",
						"--sample-rate",	preferredFrameRate.toString,
						(clampInt(preferredChannelCount, 1, 2) match {
							case 1	=> "--mono"
							case 2	=> "--stereo"
							case _	=> sys error "unexpected channelCount"
						}),
						// "--start",		"0/44100",
						// "--time",		"44100/44100",
						input.toString
					)
			_	<-	Checked.trueWin1 (
						!(res.err contains "error: frame 0: lost synchronization"),
						"file cannot be decoded"
					) orElse
					Checked.trueWin1(
						Files.exists(output) && Files.size(output) > 44,
						"output file broken"
					) leftEffect {
						_ => Files.delete(output)
					}
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".mp3")
}
