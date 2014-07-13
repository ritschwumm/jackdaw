package djane.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external faad command */
object ExternalFaad extends Decoder {
	def name	= "faad"
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("faad")
				result	<-
						execChecked(
							"faad", 
							"-i",	
							input.getPath
						)
			}
			yield {
				val extract	= extractor(result.err)
				Metadata(
					title	= extract("""title: (.*)""".r), 
					artist	= extract("""artist: (.*)""".r), 
					album	= extract("""album: (.*)""".r), 
					genre	= extract("""genre: (.*)""".r)
					// albumArtist	= extract("""album_artist: (.*)""".r), 
				)
			}
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("faad")
				_		<- requireChecked(frameRate == 44100,	"expected frameRate 44100")
				_		<- requireChecked(channelCount == 2,	"expected channelCount 2")
				_		<-
						execChecked(
							"faad", 
							"-o",	output.getPath,
							"-b",	"1",			// 16 bit signed short
							"-f",	"1",			// wav
							"-d",					// downmix => stereo:w
							"-q",
							input.getPath
						)
			}
			yield ()
	
	private val suffixChecked:File=>Checked[Unit]	=
			suffixesChecked(".m4a", ".aac")
}
