package jackdaw.media

import java.nio.file.Path

import jackdaw.util.Checked

object Opusdec extends Decoder {
	def name	= "opusdec"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil requireCommand "opusdec"
			_	<-	MediaUtil.runCommand(
						"opusdec",
						"--quiet",
						"--rate",	preferredFrameRate.toString,
						input.toString,
						output.toString
					)
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".opus")
}
