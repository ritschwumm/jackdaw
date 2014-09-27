package jackdaw.media

import java.io.File

object Flac extends Decoder {
	def name	= "flac"
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "flac"
				_	<- Checked trueWin1 (frameRate		== 44100,	"expected frameRate 44100")
				_	<- Checked trueWin1 (channelCount	== 2,		"expected channelCount 2")
				_	<-
						MediaUtil runCommand (
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
				_	<- Checked trueWin1 (output.exists, "output file not generated")
			}
			yield ()
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".flac", ".flc")
}
