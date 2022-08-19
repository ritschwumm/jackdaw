package jackdaw.media

import java.nio.file.Path
import java.nio.file.Files

import scutil.core.implicits.*
import scutil.log.*

import jackdaw.util.Checked

object Symlink extends Decoder with Logging {
	def name	= "Symlink"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_ 	<-	try {
						// TODO should ensure the wav file is compatible
						DEBUG(show"decoding with ${name}")
						Files.deleteIfExists		(output)
						Files.createSymbolicLink	(output, input)
						Right(())
					}
					catch { case e:Exception =>
						ERROR(show"${name} failed", e)
						Checked fail1 show"${name} failed: ${e.getMessage}"
					}
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".wav")
}
