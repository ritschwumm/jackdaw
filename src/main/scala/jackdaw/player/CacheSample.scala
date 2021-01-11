package jackdaw.player

import java.util.concurrent.atomic.AtomicBoolean

import scala.math._

import scutil.math.functions._

import scaudio.sample._

import jackdaw.Config

object CacheSample {
	private val chunkShift		= 12	// 4k
	private val chunkFrames		= 1<<chunkShift
	private val chunkMask		= chunkFrames-1
}

final class CacheSample(peer:Sample) extends Sample {
	import CacheSample._

	val frameRate:Int	= peer.frameRate
	val frameCount:Int	= peer.frameCount
	val sampleBytes:Int	= peer.sampleBytes

	//------------------------------------------------------------------------------

	private type Chunk	= Array[Float]

	// TODO questionable conversion
	private val spreadFrames:Int	=
		ceil((Config.preloadSpread.millis * frameRate / 1000).toDouble).toInt

	private val bufferFrames:Int	=
		spreadFrames + Player.maxDistance

	// enough chunks to fill at least bufferFrames for each head
	// scratching and fading will not occur at the same time
	// TODO this might not work with the preloadCurrent-hack
	// in the player's fadeNowOrLater
	private val bufferCount:Int	=
		ceilDivInt(bufferFrames, chunkFrames) * Player.headCount * 3 / 2

	private val lru:IntQueue		= new IntQueue(bufferCount)

	private val bufferChunks:Int	= ceilDivInt(bufferFrames, chunkFrames)

	private val chunkCount:Int		= ceilDivInt(frameCount, chunkFrames)
	private val channelCount:Int	= peer.channels.size
	private val chunkSamples:Int	= chunkFrames * channelCount
	private val chunks:Array[Chunk]	= new Array[Chunk](chunkCount)

	// println(show"buffer: ${scutil.text.Human roundedBinary chunkSamples*bufferCount*4}")

	@inline
	private def validChannelIndex(channelIndex:Int):Boolean	=
		channelIndex >= 0	&&
		channelIndex < channelCount

	@inline
	private def validChunkIndex(chunkIndex:Int):Boolean	=
		chunkIndex >= 0	&&
		chunkIndex < chunkCount

	@inline
	private def chunkIndexByFrame(frame:Int):Int	=
		if (frame >= 0)	frame / chunkFrames
		else			frame / chunkFrames - 1

	@inline
	private def firstFrameByChunk(chunkIndex:Int):Int	=
		chunkIndex * chunkFrames

	//------------------------------------------------------------------------------

	val channels:Seq[Channel]	=
		(0 until channelCount)
		.map { channelIndex =>
			new CacheChannel(channelIndex)
		}
		.toArray
		.toSeq

	private final class CacheChannel(channelIndex:Int) extends Channel {
		val frameCount:Int			= CacheSample.this.frameCount
		def get(frame:Int):Float	= CacheSample.this.getSample(frame, channelIndex)
	}

	private def getSample(frame:Int, channelIndex:Int):Float	= {
		if (!validChannelIndex(channelIndex))	return 0f

		val chunkIndex	= chunkIndexByFrame(frame)
		if (!validChunkIndex(chunkIndex))		return 0f

		val chunk	= chunks(chunkIndex)
		if (chunk eq null)						return 0f

		val sampleIndex	= (frame & chunkMask) * channelCount + channelIndex
		chunk(sampleIndex)
	}

	//------------------------------------------------------------------------------

	/** returns whether we had actual changes */
	def provide(centerFrame:Int):Boolean	= {
		// TODO what's actually loaded should depend on kind of head and ggf Player.springPitchLimit
		val center	= chunkIndexByFrame(centerFrame)
		val start	= center - bufferChunks
		val end		= center + bufferChunks
		var index	= start
		var changed	= false
		while (index < end) {
			if (validChunkIndex(index)) {
				changed	= changed | provideChunk(index)
			}
			index	+= 1
		}
		changed
	}

	@inline
	private def provideChunk(index:Int):Boolean	= {
		if (chunks(index) ne null) {
			// refresh existing chunk
			lru removeEqual index
			lru push index
			false
		}
		else if (!lru.full) {
			// allocate new chunk
			// TODO cache should be taken from a global pool
			val buffer	= new Chunk(chunkSamples)
			chunks(index)	= buffer
			lru push index
			load(index, buffer)
			true
		}
		else {
			// reuse old chunk
			val from	= lru.shift()
			val buffer	= chunks(from)
			chunks(from) 	= null
			chunks(index)	= buffer
			lru push index
			load(index, buffer)
			true
		}
	}

	private def load(chunkIndex:Int, chunk:Chunk):Unit	= {
		var inFrame		= firstFrameByChunk(chunkIndex)
		var outSample	= 0

		var frameIndex	= 0
		while (frameIndex < chunkFrames) {
			var channelIndex	= 0
			while (channelIndex < channelCount) {
				chunk(outSample)	= peer channels channelIndex get inFrame
				channelIndex	+= 1
				outSample		+= 1
			}
			frameIndex	+= 1
			inFrame		+= 1
		}
	}

	//------------------------------------------------------------------------------

	private val barrier:AtomicBoolean	= new AtomicBoolean()

	@inline
	def readBarrier():Unit	= {
        barrier.get
    }

	@inline
	def writeBarrier():Unit	= {
        barrier lazySet false
    }
}
