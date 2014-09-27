package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata

object Opusdec extends Decoder {
	def name	= "opusdec"
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "opusdec"
				_	<- Checked trueWin1 (channelCount == 2,	"expected channelCount 2")
				_	<-
						MediaUtil runCommand (
							"opusdec",
							"--quiet",
							"--rate",	frameRate.toString,
							input.getPath,
							output.getPath
						)
			}
			yield ()
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".opus")
}
