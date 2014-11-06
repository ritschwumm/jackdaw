package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Oggdec extends Decoder {
	def name	= "oggdec"
	
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "oggdec"
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
