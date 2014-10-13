package jackdaw.media

import java.io.File

object Avconv extends Decoder {
	def name	= "avconv"
				
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				// no suffix check
				_	<- MediaUtil requireCommand "avconv"
				_	<-
						MediaUtil runCommand (
							"avconv",	"-y",
							"-i",		input.getPath,
							"-vn",
							"-acodec",	"pcm_s16le",
							"-ar",		preferredFrameRate.toString,
							"-ac",		preferredChannelCount.toString,
							output.getPath
						)
			}
			yield ()
}
