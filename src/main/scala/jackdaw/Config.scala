package jackdaw

import scutil.lang.ISeq
import scutil.time.implicits._

import scaudio.output._
import scaudio.dsp.BiQuadCoeffs

import jackdaw.audio.PitchMath._

object Config {
	val updateTick		= 40.millis 
	
	val outputConfig	= OutputConfig(ISeq.empty, 44100, 512, 8, true)
	
	// (output.rate * Config.updateTick.millis / 1000).toInt / 4
	val controlFrames	= 256
	
	// how much more block should be in the engine feed back queue after update
	val queueOvershot	= 10
	
	// assumed size of a block on disk
	val diskBlockSize	= 4096
	
	// TODO should depend on BPM and loop size
	val preloadTime		= 2.seconds
	
	// correction factor for the feedback rate 
	val rateFactor		= 1.005	// 0.5%
	
	val sincEnabled		= true
	
	val lowEq			= 880	// Hz
	val highEq			= 5000	// Hz
	
	val filterLow		= 20.0	// Hz above zero
	val filterHigh		= 10.0	// Hz below nyquist
	val filterQ			= BiQuadCoeffs.sqrt2half * 1.5
	
	val detectBpsRange	= (bpm(60.0), bpm(220.0))
	val rhythmBpsRange	= (bpm(60.0), bpm(220.0))
	
	val curveRaster		= 256
	
	// TODO rethink these
	
	// NOTE must be greater than the number of decks
	val curTrackCount	= 4
	
	val minCacheCount	= curTrackCount * 2
	
	require(minCacheCount > curTrackCount)
	
	val maxCacheSize	= 10 * 1024L * 1024L * 1024L	// 10 gig
}
