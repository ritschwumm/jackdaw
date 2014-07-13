package djane.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external madplay command */
object ExternalMadplay extends Decoder {
	def name	= "madplay"
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("madplay")
				result	<-
						execChecked(
							"madplay", 
							"-T",
							input.getPath
						)
			}
			yield {
				val extract	= extractor(result.err)
				Metadata(
					title	= extract("""\s*Title: (.*)""".r), 
					artist	= extract("""\s*Artist: (.*)""".r), 
					album	= extract("""\s*Album: (.*)""".r), 
					genre	= extract("""\s*Genre: (.*)""".r)
					// publisher	= extract("""\s*Publisher: (.*)""".r),
					// comment		= extract("""\s*Comment: (.*)""".r),
				)
			}
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("faad")
				_		<- requireChecked(channelCount >= 1,	"expected channelCount >= 1")
				_		<- requireChecked(channelCount <= 2,	"expected channelCount <= 2")
				_		<-
						execChecked(
							"madplay", 
							"--output",			"wav:" + output.getPath,
							"--bit-depth",		"16",
							"--sample-rate",	frameRate.toString,
							(channelCount match {
								case 1	=> "--mono"
								case 2	=> "--stereo"
								case _	=> sys error ("unexpected channelCount: " + channelCount)
							}),
							// "--start",		"0/44100",
							// "--time",		"44100/44100",
							input.getPath
						)
			}
			yield ()
			
	private val suffixChecked:File=>Checked[Unit]	=
			suffixesChecked(".mp3")
}
