package jackdaw.media

import java.io.File
import java.io.RandomAccessFile

import de.jarnbjo.ogg._
import de.jarnbjo.vorbis._

import scala.annotation.tailrec

import scutil.lang._
import scutil.implicits._
import scutil.io.Charsets._
import scutil.math.ByteArrayUtil
import scutil.log._

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object JOgg extends Inspector with Decoder with Logging {
	def name	= "j-ogg"
	
	private implicit def PhysicalOggStreamIsDisposable(physical:PhysicalOggStream):Disposable	=
			disposable { physical.close() }
		
	private type BufferWriter	= (Array[Byte], Int) => Unit
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_	<- recognizeFile(input)
				out	<-
						withVorbisStream(input) { vorbis =>
							DEBUG(s"reading metadata with ${name}")
							val header		= vorbis.getCommentHeader
							Win(Metadata(
								title	= header.getTitle.guardNotNull,
								artist	= header.getArtist.guardNotNull,
								album	= header.getAlbum.guardNotNull
								// genre	= header.getGenre.guardNotNull
							))
						}		
			}
			yield out
			
	// BETTER use scalaz, this is a mess. should be using a Resource monad transformer
	def convertToWav(input:File, output:File, preferredFrameRate:Int, preferredChannelCount:Int):Checked[Unit] =
			for {
				_	<- recognizeFile(input)
				_	<-
					withVorbisStream(input) { vorbis =>
						DEBUG(s"decoding with ${name}")
						val header	= vorbis.getIdentificationHeader
						for {
							_		<-
									writeWavChecked(output, header.getSampleRate, header.getChannels.toShort) { append:BufferWriter =>
										copyPcm(vorbis, append)
									} failEffect {
										_ => output.delete()
									}
						}
						yield (())
					}
			}
			yield ()
			
	private def copyPcm(vorbisStream:VorbisStream, append:(Array[Byte], Int)=>Unit) {
		val buffer	= new Array[Byte](16384)
		while (true) {
			try {
				val len = vorbisStream readPcm (buffer, 0, buffer.length)
				// BETTER use AudioFormat_S2LE.putShort?
				// convert to little endian
				swapEndianShort(buffer, len)
				append(buffer, len)
			}
			catch { case e:EndOfOggStreamException =>
				return
			}
		}
	}
	
	private def swapEndianShort(it:Array[Byte], byteCount:Int) {
		var i	= 0
		while (i < byteCount) {
			val h	= it(i)
			val l	= it(i+1)
			it(i)	= l
			it(i+1)	= h
			i		+= 2
		}
	}
			
	private def withVorbisStream[T](file:File)(func:VorbisStream=>Checked[T]):Checked[T]	=
			withLogicalOggStream(file) { logical =>
				func(new VorbisStream(logical))
			}
			
	private def withLogicalOggStream[T](file:File)(func:LogicalOggStream=>Checked[T]):Checked[T]	=
			MediaUtil checkedExceptions {
				new FileStream(new RandomAccessFile(file, "r")) use { physical =>
					for {
						logical	<-
								physical.getLogicalStreams.toIterable.singleOption
								.map	{ _.asInstanceOf[LogicalOggStream] }
								.toWin	(Checked problem1 "expected exactly one logical stream")
						out		<- func(logical)
					}
					yield out
				}
			}
			
	private def writeWavChecked(output:File, frameRate:Int, channelCount:Short)(generator:Effect[BufferWriter])	=
			MediaUtil checkedExceptions {
				writeWav(output, frameRate, channelCount.toShort)(generator)
				Win(())
			}
		
	// generator must provide interleaved little endian signed shorts
	private def writeWav(output:File, frameRate:Int, channelCount:Short)(generator:Effect[BufferWriter]) {
		new RandomAccessFile(output, "rw") use { outFile =>
			val UIntMaxValue	= 1L<<32-1
			
			def writeId(it:String):Unit	= {
				require(it.length == 4, s"tag id expected to have 4 chars, ${it} has ${it.length}")
				outFile write (it getBytes us_ascii)
			}
			def writeInt(it:Int):Unit						= outFile write (ByteArrayUtil littleEndianInt		it)
			def writeShort(it:Short):Unit					= outFile write (ByteArrayUtil littleEndianShort	it)
			def writeBytes(bytes:Array[Byte], len:Int):Unit	= outFile write (bytes, 0, len)
			
			def tag(id:String)(content: =>Unit) {
				writeId(id)
				val lengthPos	= outFile.length
				writeInt(0)
				val startPos	= outFile.length
				content
				val endPos	= outFile.length
				outFile seek lengthPos
				val tagSize	= endPos - startPos
				// TODO use Tried
				require (tagSize <= UIntMaxValue, "maximum RIFF tag size exceeded")
				writeInt(tagSize.toInt)
				// add padding
				if (tagSize%2 == 1) {
					outFile write 0
				}
				outFile seek endPos
			}
	
			val WAVE_FORMAT_PCM			= 1.toShort
			
			val bitsPerSample:Short		= 16.toShort
			val bytesPerSample:Short	= ((bitsPerSample + 7) / 8).toShort
			val bytesPerFrame:Short		= (channelCount*bytesPerSample).toShort
			
			outFile setLength 0L
			
			tag("RIFF") {
				writeId("WAVE")
				tag("fmt ") {
					writeShort(WAVE_FORMAT_PCM)
					writeShort(channelCount)
					writeInt(frameRate)
					writeInt(frameRate*bytesPerFrame)
					writeShort(bytesPerFrame)
					writeShort(bitsPerSample)
				}
				tag("data") {
					generator(writeBytes)
				}
			}
		}				
	}
	
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".ogg")
}
