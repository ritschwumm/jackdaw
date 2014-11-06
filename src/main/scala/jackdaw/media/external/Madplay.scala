package jackdaw.media

import java.io.File

import scutil.math.clampInt

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Madplay extends Inspector with Decoder {
	def name	= "madplay"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				_		<- MediaUtil requireCommand "madplay"
				result	<-
						MediaUtil runCommand (
							"madplay", 
							"-T",
							input.getPath
						)
			}
			yield {
				val extract	= MediaUtil extractFrom result.err
				Metadata(
					title	= extract("""\s*Title: (.*)""".r), 
					artist	= extract("""\s*Artist: (.*)""".r), 
					album	= extract("""\s*Album: (.*)""".r)
					// genre		= extract("""\s*Genre: (.*)""".r)
					// publisher	= extract("""\s*Publisher: (.*)""".r),
					// comment		= extract("""\s*Comment: (.*)""".r),
				)
			}
	
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "madplay"
				res	<-
						MediaUtil runCommand (
							"madplay", 
							"--output",			"wav:" + output.getPath,
							"--bit-depth",		"16",
							"--sample-rate",	preferredFrameRate.toString,
							(clampInt(preferredChannelCount, 1, 2) match {
								case 1	=> "--mono"
								case 2	=> "--stereo"
								case _	=> sys error "unexpected channelCount"
							}),
							// "--start",		"0/44100",
							// "--time",		"44100/44100",
							input.getPath
						)
				_	<-
						(Checked trueWin1 (
							!(res.err contains "error: frame 0: lost synchronization"), 
							"file cannot be decoded"
						)) orElse
						(Checked trueWin1 (
							output.length > 44, 
							"output file broken"
						)) failEffect { 
							_ => output.delete() 
						}
			}
			yield ()
			
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".mp3")
}
