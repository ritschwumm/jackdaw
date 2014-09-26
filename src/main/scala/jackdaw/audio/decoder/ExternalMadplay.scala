package jackdaw.audio.decoder

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import jackdaw.audio.Metadata

/** interface to an external madplay command */
object ExternalMadplay extends Decoder {
	def name	= "madplay"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandAvailable("madplay")
				result	<-
						exec(
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
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- suffixChecked(input)
				_	<- commandAvailable("madplay")
				_	<- requirement(channelCount >= 1,	"expected channelCount >= 1")
				_	<- requirement(channelCount <= 2,	"expected channelCount <= 2")
				res	<-
						exec(
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
				_	<-
						requirement(
							!(res.err contains "error: frame 0: lost synchronization"), 
							"file cannot be decoded"
						) orElse
						requirement(
							output.length > 44, 
							"output file broken"
						) failEffect { 
							_ => output.delete() 
						}
			}
			yield ()
			
	private val suffixChecked:File=>Checked[Unit]	=
			suffixIn(".mp3")
}
