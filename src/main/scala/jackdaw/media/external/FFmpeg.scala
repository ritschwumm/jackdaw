package jackdaw.media

import java.io.File

import jackdaw.util.Checked

object FFmpeg extends Decoder {
	def name	= "ffmpeg"

	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			// no suffix check
			_	<-	MediaUtil requireCommand "ffmpeg"
			_	<-	MediaUtil runCommand (
						"ffmpeg",	"-y",
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
