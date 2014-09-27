package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata

object Mpg123 extends Inspector with Decoder {
	def name	= "mpg123"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				_		<- MediaUtil requireCommand "mpg123"
				result	<-
						MediaUtil runCommand (
							"mpg123", 
							"-n",	"1",
							// "--stdout",
							"-w",	"-",
							input.getPath
						)
			}
			yield {
				val extract	= MediaUtil extractFrom result.err
				Metadata(
					title	= extract("""Title:\s+(.*?)\s+Artist:\s+(?:.*)""".r), 
					artist	= extract("""Title:\s+(?:.*?)\s+Artist:\s+(.*)""".r), 
					album	= extract("""Album:\s+(.*)""".r) 
					// genre	= extract("""Genre:\s+(.*)""".r)
					// MPEG 1.0 layer III, 192 kbit/s, 44100 Hz stereo
				)
			}
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "mpg123"
				_	<- Checked trueWin1 (channelCount >= 1,	"expected channelCount >= 1")
				_	<- Checked trueWin1 (channelCount <= 2,	"expected channelCount <= 2")
				_	<-
						MediaUtil runCommand (
							"mpg123", 
							"-w",	output.getPath,	
							// -8bit
							"-r",	frameRate.toString,
							(channelCount match {
								case 1	=> "--mono"
								case 2	=> "--stereo"
								case _	=> sys error ("unexpected channelCount: " + channelCount)
							}),
							input.getPath
						)
			}
			yield ()
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".mp3")
}
