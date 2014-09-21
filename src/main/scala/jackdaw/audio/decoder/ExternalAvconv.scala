package jackdaw.audio.decoder

import java.io._

import scala.util.matching.Regex

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import jackdaw.audio.Metadata

/** interface to an external faad command */
object ExternalAvconv extends Decoder {
	def name	= "avprobe/avconv"
				
	def readMetadata(input:File):Checked[Metadata] =
			for {
				// no suffix check
				_		<- commandAvailable("avprobe")
				result	<-
						exec(
							"avprobe",	input.getPath
							// this no longer works
							/*
							"avconv",	"-y",		
							"-i",		input.getPath,
							"-vn",		"-an",
							"-null",	"-"
							*/
						)
			}
			yield {
				val extract	= extractor(result.err)
				Metadata(
					title	= extract("""    title\s*: (.*)""".r), 
					artist	= extract("""    artist\s*: (.*)""".r), 
					album	= extract("""    album\s*: (.*)""".r), 
					genre	= extract("""    genre\s*: (.*)""".r)
				)
			}
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				// no suffix check
				_	<- commandAvailable("avconv")
				_	<-
						exec(
							"avconv",	"-y",
							"-i",		input.getPath,
							"-vn",
							"-acodec",	"pcm_s16le",
							"-ar",		frameRate.toString,
							"-ac",		channelCount.toString,
							output.getPath
						)
			}
			yield ()
}
