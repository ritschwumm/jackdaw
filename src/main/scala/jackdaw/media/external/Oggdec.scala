package jackdaw.media

import java.nio.file.Path

import jackdaw.util.Checked

object Oggdec extends Decoder {
	def name	= "oggdec"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil requireCommand "oggdec"
			_	<-	MediaUtil.runCommand(
						"oggdec", 	// or ogg123
						"-o",	output.toString,
						"-b",	"16",			// 16 bit
						"-e",	"0",			// little endian
						"-s",	"1",			// signed
						// "-R",				// raw
						"-q",					// quiet
						input.toString
					)
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".ogg")
}
