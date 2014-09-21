package jackdaw.audio.decoder

import java.io._

import javazoom.jl.decoder._
import javazoom.jl.decoder.Decoder.{ Params => DecoderParams }
import javazoom.jl.converter._
import javazoom.jl.converter.Converter.{ ProgressListener => ConverterProgressListener }

import scutil.lang._
import scutil.implicits._
import scutil.log._

import jackdaw.audio.Metadata

/** use JLayer to decode mp3 files */
object InternalJLayer extends Decoder {
	def name	= "JLayer"
	
	// TODO implement
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_	<- suffixChecked(input)
			}
			yield {
				Metadata(
					title	= None,
					artist	= None,
					album	= None,
					genre	= None
				)
			}
	
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit] =
			for {
				_	<- suffixChecked(input)
				_	<- requirement(frameRate == 44100,	"expected frameRate 44100")
				_	<- requirement(channelCount == 2,	"expected channelCount 2")
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
							Fail(ISeq(s"${name} failed: ${e.getMessage}"))
						}
					}
			}
			yield ()
			
	private val suffixChecked:File=>Checked[Unit]	=
			suffixIn(".mp3")
}
