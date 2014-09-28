package jackdaw.media

import java.io.File

import jackdaw.audio.Metadata

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
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- MediaUtil requireCommand "madplay"
				_	<- Checked trueWin1 (channelCount >= 1,	"expected channelCount >= 1")
				_	<- Checked trueWin1 (channelCount <= 2,	"expected channelCount <= 2")
				res	<-
						MediaUtil runCommand (
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