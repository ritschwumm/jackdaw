package jackdaw.media

import java.nio.file.Path

import jackdaw.util.Checked

object Decoder {
	val all:Seq[Decoder]	=
		Vector[Decoder](
			Symlink,
			Madplay,
			Mpg123,
			Faad,
			Oggdec,
			Flac,
			Opusdec,
			FFmpeg,
			Avconv,
			JLayer,
			JOgg
		)

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Boolean	=
		MediaUtil
		.worker[Decoder,Unit](
			all,
			_.name,
			_.convertToWav(input, output, preferredFrameRate, preferredChannelCount)
		)
		.isDefined
}

trait Decoder {
	def name:String

	/** decode file and write into a wav file */
	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit]
}
