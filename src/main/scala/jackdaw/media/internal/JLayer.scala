package jackdaw.media

import java.io.File

import javazoom.jl.decoder._
import javazoom.jl.decoder.Decoder.{ Params => DecoderParams }
import javazoom.jl.converter._
import javazoom.jl.converter.Converter.{ ProgressListener => ConverterProgressListener }

import scutil.lang._
import scutil.implicits._
import scutil.log._

object JLayer extends Decoder with Logging {
	def name	= "JLayer"
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<- Checked trueWin1 (frameRate == 44100,	"expected frameRate 44100")
				_	<- Checked trueWin1 (channelCount == 2,	"expected channelCount 2")
				_ 	<-
					input withInputStream { ist =>
						DEBUG(s"decoding with ${name}")
						val pl:ConverterProgressListener	 = new ConverterProgressListener {
							def converterUpdate(updateID:Int, param1:Int, param2:Int):Unit	= {}
							def parsedFrame(frameNo:Int, header:Header):Unit				= {}
							def readFrame(frameNo:Int, header:Header):Unit					= {}
							def decodedFrame(frameNo:Int, header:Header, o:Obuffer):Unit	= {}
							def converterException(t:Throwable):Boolean	= false
						}
						val dp:DecoderParams	= null
						try {
							new Converter convert (ist, output.getAbsolutePath, pl, null)
							Win(())
						}
						catch { case e:JavaLayerException =>
							ERROR(s"${name} failed", e)
							Checked fail1 s"${name} failed: ${e.getMessage}"
						}
					}
			}
			yield ()
			
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".mp3")
}
