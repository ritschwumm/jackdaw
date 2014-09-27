package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata

object Oggdec extends Decoder {
	def name	= "oggdec"
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "oggdec"
				_	<- Checked trueWin1 (frameRate		== 44100,	"expected frameRate 44100")
				_	<- Checked trueWin1 (channelCount	== 2,		"expected channelCount 2")
				_	<-
						MediaUtil runCommand (
							"oggdec", 	// or ogg123
							"-o",	output.getPath,
							"-b",	"16",			// 16 bit
							"-e",	"0",			// little endian
							"-s",	"1",			// signed
							// "-R",				// raw
							"-q",					// quiet
							input.getPath
						)
			}
			yield ()
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".ogg")
}
