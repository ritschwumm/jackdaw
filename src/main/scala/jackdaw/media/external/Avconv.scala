package jackdaw.media

import java.nio.file.Path

import jackdaw.util.Checked

object Avconv extends Decoder {
	def name	= "avconv"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			// no suffix check
			_	<- MediaUtil.requireCommand("avconv")
			_	<-
					MediaUtil.runCommand(
						"avconv",	"-y",
						"-i",		input.toString,
						"-vn",
						"-acodec",	"pcm_s16le",
						"-ar",		preferredFrameRate.toString,
						"-ac",		preferredChannelCount.toString,
						output.toString
					)
		}
		yield ()
}
