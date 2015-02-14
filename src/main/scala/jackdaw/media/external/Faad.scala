package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Faad extends Inspector with Decoder {
	def name	= "faad"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				_		<- MediaUtil requireCommand "faad"
				result	<-
						MediaUtil runCommand (
							"faad",
							"-i",	
							input.getPath
						)
			}
			yield {
				val extract	= MediaUtil extractFrom result.err
				Metadata(
					title	= extract("""title: (.*)""".r),
					artist	= extract("""artist: (.*)""".r),
					album	= extract("""album: (.*)""".r)
					// genre		= extract("""genre: (.*)""".r)
					// albumArtist	= extract("""album_artist: (.*)""".r),
				)
			}
	
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "faad"
				_	<-
						MediaUtil runCommand (
							"faad",
							"-o",	output.getPath,
							"-b",	"1",			// 16 bit signed short
							"-f",	"1",			// wav
							"-d",					// downmix => stereo:w
							"-q",
							input.getPath
						)
				// NOTE faad rc is 0 regardless of whether it worked or not
				_	<- Checked trueWin1 (output.exists, "output file not generated")
			}
			yield ()
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".m4a", ".aac")
}
