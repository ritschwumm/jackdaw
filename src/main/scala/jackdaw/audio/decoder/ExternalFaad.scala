package jackdaw.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import jackdaw.audio.Metadata

/** interface to an external faad command */
object ExternalFaad extends Decoder {
	def name	= "faad"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognized(input)
				_		<- commandAvailable("faad")
				result	<-
						exec(
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
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognized(input)
				_	<- commandAvailable("faad")
				_	<- requirement(frameRate == 44100,	"expected frameRate 44100")
				_	<- requirement(channelCount == 2,	"expected channelCount 2")
				_	<-
						exec(
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
	
	private val recognized:File=>Checked[Unit]	=
			suffixIn(".m4a", ".aac")
}
