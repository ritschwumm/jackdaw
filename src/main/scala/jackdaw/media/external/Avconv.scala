package jackdaw.media

import java.io.File

object Avconv extends Decoder {
	def name	= "avconv"
				
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				// no suffix check
				_	<- MediaUtil requireCommand "avconv"
				_	<-
						MediaUtil runCommand (
							"avconv",	"-y",
							"-i",		input.getPath,
							"-vn",
							"-acodec",	"pcm_s16le",
							"-ar",		frameRate.toString,
							"-ac",		channelCount.toString,
							output.getPath
						)
			}
			yield ()
}
