package jackdaw.media

import java.nio.file.Path

import scutil.jdk.implicits.*
import scutil.math.functions.*

import jackdaw.util.Checked

object Mpg123 extends Inspector with Decoder {
	def name	= "mpg123"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			_		<-	MediaUtil.requireCommand("mpg123")
			result	<-	MediaUtil.runCommand(
							"mpg123",
							"-n",	"1",
							// "--stdout",
							"-w",	"-",
							input.toString
						)
		}
		yield {
			val extract	= MediaUtil.extractFrom(result.err)
			Metadata(
				title	= extract(re"""Title:\s+(.*?)\s+Artist:\s+(?:.*)"""),
				artist	= extract(re"""Title:\s+(?:.*?)\s+Artist:\s+(.*)"""),
				album	= extract(re"""Album:\s+(.*)""")
				// genre	= extract(re"""Genre:\s+(.*)""")
				// MPEG 1.0 layer III, 192 kbit/s, 44100 Hz stereo
			)
		}

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MediaUtil.requireCommand("mpg123")
			_	<-	MediaUtil.runCommand(
						"mpg123",
						"-w",	output.toString,
						// -8bit
						"-r",	preferredFrameRate.toString,
						(clampInt(preferredChannelCount, 1, 2) match {
							case 1	=> "--mono"
							case 2	=> "--stereo"
							case _	=> sys error "unexpected channelCount"
						}),
						input.toString
					)
		}
		yield ()

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".mp3")
}
