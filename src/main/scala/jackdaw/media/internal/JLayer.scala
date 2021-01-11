package jackdaw.media

import java.io.File

import javazoom.jl.decoder.{Header,Obuffer,JavaLayerException}
import javazoom.jl.converter._
import javazoom.jl.converter.Converter.{ ProgressListener => ConverterProgressListener }

import scutil.core.implicits._
import scutil.jdk.implicits._
import scutil.log._

import jackdaw.util.Checked

object JLayer extends Decoder with Logging {
	def name	= "JLayer"

	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_ 	<-	input withInputStream { ist =>
						DEBUG(show"decoding with ${name}")

						try {
							(new Converter).convert(ist, output.getAbsolutePath, pl, null)
							Right(())
						}
						catch { case e:JavaLayerException =>
							ERROR(show"${name} failed", e)
							Checked fail1 show"${name} failed: ${e.getMessage}"
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
