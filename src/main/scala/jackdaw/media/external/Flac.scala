package jackdaw.media

import java.io.File

import jackdaw.util.Checked

object Flac extends Decoder {
	def name	= "flac"

	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil requireCommand "flac"
			_	<-	MediaUtil runCommand (
						"flac",
						"-o",				output.getPath,
						"--decode",			// decode
						// "--bps",			"16",					// 16 bit
						// "--endian",		"little",				// little endian
						// "--sign",		"signed",				// signed
						// "--channels",	channelCount.toString,	// channels
						// "--sample-rate",	frameRate.toString,		// frame rate
						"-f",										// overwrite existing output
						"-F",										// keep decoding on errors
						// --skip --until --cue
						"-s",					// silent
						input.getPath
					)
			// NOTE flac rc is 0 regardless of whether it worked or not
			_	<-	Checked trueWin1 (output.exists, "output file not generated")
		}
		yield ()

	private val recognizeFile:File=>Checked[Unit]	=
		MediaUtil requireFileSuffixIn (".flac", ".flc")
}
