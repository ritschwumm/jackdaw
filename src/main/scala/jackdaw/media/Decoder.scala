package jackdaw.media

import java.io.File

import scutil.lang._

import jackdaw.util.Checked

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
	
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Boolean	=
		MediaUtil
			.worker[Decoder,Unit](
				all,
				_.name,
				_ convertToWav (input, output, preferredFrameRate, preferredChannelCount)
			)
			.isDefined
}

trait Decoder {
	def name:String
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit]
}
