package jackdaw.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import jackdaw.audio.Metadata

/** interface to an external oggdec and vorbiscomment commands */
object ExternalVorbisTools extends Decoder {
	def name	= "oggdec/vorbiscomment"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognized(input)
				_		<- commandAvailable("vorbiscomment")
				result	<-
						exec(
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
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognized(input)
				_	<- commandAvailable("oggdec")
				_	<- requirement(frameRate == 44100,	"expected frameRate 44100")
				_	<- requirement(channelCount == 2,	"expected channelCount 2")
				_	<-
						exec(
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
	
	private val recognized:File=>Checked[Unit]	=
			suffixIn(".ogg")
}
