package djane.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external madplay command */
object ExternalMpg123 extends Decoder {
	def name	= "mpg123"
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("mpg123")
				result	<-
						execChecked(
							"mpg123", 
							"-n",	"1",
							// "--stdout",
							"-w",	"-",
							input.getPath
						)
			}
			yield {
				val extract	= extractor(result.err)
				Metadata(
					title	= extract("""Title:\s+(.*?)\s+Artist:\s+(?:.*)""".r), 
					artist	= extract("""Title:\s+(?:.*?)\s+Artist:\s+(.*)""".r), 
					album	= extract("""Album:\s+(.*)""".r), 
					genre	= extract("""Genre:\s+(.*)""".r)
					// MPEG 1.0 layer III, 192 kbit/s, 44100 Hz stereo
				)
			}
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("mpg123")
				_		<- requireChecked(channelCount >= 1,	"expected channelCount >= 1")
				_		<- requireChecked(channelCount <= 2,	"expected channelCount <= 2")
				_		<-
						execChecked(
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
	
	private val suffixChecked:File=>Checked[Unit]	=
			suffixesChecked(".mp3")
}
