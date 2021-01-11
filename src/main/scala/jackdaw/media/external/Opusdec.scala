package jackdaw.media

import java.io.File

import jackdaw.util.Checked

object Opusdec extends Decoder {
	def name	= "opusdec"

	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil requireCommand "opusdec"
			_	<-	MediaUtil.runCommand(
						"opusdec",
						"--quiet",
						"--rate",	preferredFrameRate.toString,
						input.getPath,
						output.getPath
					)
		}
		yield ()

	private val recognizeFile:File=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".opus")
}
