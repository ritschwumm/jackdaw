package jackdaw.audio.decoder

import java.io._

import javazoom.jl.decoder._
import javazoom.jl.decoder.Decoder.{ Params => DecoderParams }
import javazoom.jl.converter._
import javazoom.jl.converter.Converter.{ ProgressListener => ConverterProgressListener }

import com.mpatric.mp3agic._

import scutil.lang._
import scutil.implicits._
import scutil.log._

import jackdaw.audio.Metadata

/** use JLayer to decode mp3 files and mp3agic to read metadata */
object InternalJLayerMp3agic extends Decoder {
	def name	= "JLayer/Mp3agic"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- suffixChecked(input)
				mp3file	<-
						Catch.exception in new Mp3File(input.getAbsolutePath) mapFail { e =>
							ERROR("cannot create Mp3File", e)
							ISeq("cannot create Mp3File")
						}
						
				v1		= mp3file.hasId3v1Tag guard mp3file.getId3v1Tag
				title1	= v1 flatMap { _.getTitle.guardNotNull	}
				artist1	= v1 flatMap { _.getArtist.guardNotNull	}
				album1	= v1 flatMap { _.getAlbum.guardNotNull	}
				genre1	= v1 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }
				
				v2		= mp3file.hasId3v2Tag guard mp3file.getId3v2Tag
				title2	= v2 flatMap { _.getTitle.guardNotNull	}
				artist2	= v2 flatMap { _.getArtist.guardNotNull	}
				album2	= v2 flatMap { _.getAlbum.guardNotNull	}
				genre2	= v2 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }
			}
			yield {
				Metadata(
					title	= title2	orElse title1, 
					artist	= artist2	orElse artist1,
					album	= album2	orElse album1,
					genre	= genre2	orElse genre1
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
