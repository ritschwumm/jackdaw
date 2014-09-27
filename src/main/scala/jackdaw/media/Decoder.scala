package jackdaw.media

import java.io.File

import scutil.lang._

object Decoder {
	val all:ISeq[Decoder]	=
			Vector(
				Madplay,
				Mpg123,
				Faad, 
				Oggdec,
				Flac,
				Opusdec,
				Avconv,
				JLayer,
				JOgg
			)
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Boolean	=
			MediaUtil
			.worker[Decoder,Unit](
				all,
				_.name,
				_ convertToWav (input, output, frameRate, channelCount)
			)
			.isDefined
}

trait Decoder {
	def name:String
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit]
}
