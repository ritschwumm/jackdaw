package jackdaw.media

import java.io.File

import javazoom.jl.decoder._
import javazoom.jl.converter._
import javazoom.jl.converter.Converter.{ ProgressListener => ConverterProgressListener }

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.lang.{ Converter => _, _ }
import scutil.log._

import jackdaw.util.Checked

object JLayer extends Decoder with Logging {
	def name	= "JLayer"
	
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_ 	<-
					input withInputStream { ist =>
						DEBUG(so"decoding with ${name}")
						
						try {
							new Converter convert (ist, output.getAbsolutePath, pl, null)
							Win(())
						}
						catch { case e:JavaLayerException =>
							ERROR(so"${name} failed", e)
							Checked fail1 so"${name} failed: ${e.getMessage}"
						}
					}
			}
			yield ()
			
	private val pl:ConverterProgressListener =
			new ConverterProgressListener {
				def converterUpdate(updateID:Int, param1:Int, param2:Int):Unit	= {}
				def parsedFrame(frameNo:Int, header:Header):Unit				= {}
				def readFrame(frameNo:Int, header:Header):Unit					= {}
				def decodedFrame(frameNo:Int, header:Header, o:Obuffer):Unit	= {}
				def converterException(t:Throwable):Boolean	= false
			}
			
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".mp3")
}
