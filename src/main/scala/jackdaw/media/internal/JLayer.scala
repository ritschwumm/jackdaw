package jackdaw.media

import java.nio.file.Path

import javazoom.jl.decoder.{Header,Obuffer,JavaLayerException}
import javazoom.jl.converter.*
import javazoom.jl.converter.Converter.{ ProgressListener as ConverterProgressListener }

import scutil.core.implicits.*
import scutil.log.*
import scutil.io.*

import jackdaw.util.Checked

object JLayer extends Decoder with Logging {
	def name	= "JLayer"

	def convertToWav(input:Path, output:Path, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
		for {
			_	<-	recognizeFile(input)
			_	<-	MoreFiles.withInputStream(input) { ist =>
						DEBUG(show"decoding with ${name}")

						try {
							(new Converter).convert(ist, output.normalize.toString, pl, null)
							Right(())
						}
						catch { case e:JavaLayerException =>
							ERROR(show"${name} failed", e)
							Checked.fail1(show"${name} failed: ${e.getMessage}")
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

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".mp3")
}
