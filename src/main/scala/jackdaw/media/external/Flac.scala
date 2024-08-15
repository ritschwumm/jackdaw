package jackdaw.media

import java.nio.file.*

import jackdaw.util.Checked

object Flac extends Decoder {
	def name	= "flac"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil.requireCommand("flac")
			_	<-	MediaUtil.runCommand(
						"flac",
						"-o",				output.toString,
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
						input.toString
					)
			// NOTE flac rc is 0 regardless of whether it worked or not
			_	<-	Checked.trueWin1(Files.exists(output), "output file not generated")
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".flac", ".flc")
}
