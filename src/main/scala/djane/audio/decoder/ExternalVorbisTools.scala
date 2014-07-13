package djane.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external oggdec and vorbiscomment commands */
object ExternalVorbisTools extends Decoder {
	def name	= "oggdec/vorbiscomment"
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("vorbiscomment")
				result	<-
						execChecked(
							"vorbiscomment", 
							input.getPath
						)
			}
			yield {
				val extract	= extractor(result.out)
				Metadata(
					title	= extract("""TITLE=(.*)""".r), 
					artist	= extract("""ARTIST=(.*)""".r), 
					album	= extract("""ALBUM=(.*)""".r), 
					genre	= extract("""GENRE=(.*)""".r)
				)
			}
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("oggdec")
				_		<- requireChecked(frameRate == 44100,	"expected frameRate 44100")
				_		<- requireChecked(channelCount == 2,	"expected channelCount 2")
				_		<-
						execChecked(
							"oggdec", 	// or ogg123
							"-o",	output.getPath,
							"-b",	"16",			// 16 bit
							"-e",	"0",			// little endian
							"-s",	"1",			// signed
							// "-R",				// raw
							"-q",					// quiet
							input.getPath
						)
			}
			yield ()
	
	private val suffixChecked:File=>Checked[Unit]	=
			suffixesChecked(".ogg")
}
