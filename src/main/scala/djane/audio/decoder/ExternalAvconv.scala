package djane.audio.decoder

import java.io._

import scala.util.matching.Regex

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import djane.audio.Metadata

/** interface to an external faad command */
object ExternalAvconv extends Decoder {
	def name	= "avprobe/avconv"
				
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("avprobe")
				result	<-
						execChecked(
							"avprobe",	input.getPath
							/*
							"avconv",	"-y",		
							"-i",		input.getPath,
							"-vn",		"-an",
							"-null",	"-"	// this no longer works
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
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_		<- suffixChecked(input)
				_		<- commandChecked("avconv")
				result	<-
						execChecked(
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
	
	// NOTE the catch-all
	private def suffixChecked(input:File):Checked[Unit]	=
			Win(())
}
