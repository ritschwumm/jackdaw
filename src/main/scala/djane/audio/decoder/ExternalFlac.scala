package djane.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external flac command */
object ExternalFlac extends Decoder {
	def name	= "flac/metaflac"
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("metaflac")
				result	<-
						execChecked(
							"metaflac",
							"--list",
							input.getPath
						)
			}
			yield {
				val extract	= extractor(result.out)
				Metadata(
					title	= extract(""".*TITLE=(.*)""".r), 
					artist	= extract(""".*ARTIST=(.*)""".r), 
					album	= extract(""".*ALBUM=(.*)""".r), 
					genre	= extract(""".*GENRE=(.*)""".r)
				)
			}
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("flac")
				_		<- requireChecked(frameRate == 44100,	"expected frameRate 44100")
				_		<- requireChecked(channelCount == 2,	"expected channelCount 2")
				_		<-
						execChecked(
							"flac",
							"-o",				output.getPath,
							"--decode",			// decode
							// "--bps",			"16",					// 16 bit
							// "--endian",		"little",				// little endian
							// "--sign",		"signed",				// signed
							// "--channels",	channelCount.toString,	// channels
							// "--sample-rate",	frameRate.toString,		// frame rate
							"-f",										// overwrite existing output
							"-F",										// keep decoding on errors
							// --skip --until --cue
							"-s",					// silent
							input.getPath
						)
			}
			yield ()
	
	private val suffixChecked:File=>Checked[Unit]	=
			suffixesChecked(".flac", ".flc")
}
