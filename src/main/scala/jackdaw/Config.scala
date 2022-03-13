package jackdaw

import scutil.core.implicits.*

import scaudio.output.*
import scaudio.dsp.BiQuadCoeffs

import jackdaw.range.PitchMath.*
import jackdaw.util.AppDirs

object Config {
	val guiUpdateInterval		= 40.millis

	// how much more block should be in the engine feed back queue after update
	val guiQueueOvershot		= 10

	// correction factor for the feedback rate
	val guiQueueAdaptFactor		= 1.005	// 0.5%

	// (output.rate * Config.updateTick.millis / 1000).toInt / 4
	// how often to talk to the gui
	val guiIntervalFrames		= 256

	// how often to talk to the loader
	val loaderIntervalFrames	= 1024

	val preloadSpread			= 1.seconds

	val outputConfig	= OutputConfig(Seq.empty, 44100, 1024, 4, true)

	val sincEnabled		= true

	val lowEq			= 880	// Hz
	val highEq			= 5000	// Hz

	val filterLow		= 20.0	// Hz above zero
	val filterHigh		= 10.0	// Hz below nyquist
	val filterQ			= BiQuadCoeffs.sqrt2half * 1.5

	val detectBpsRange	= (bpm(60.0), bpm(220.0))
	val rhythmBpsRange	= (bpm(60.0), bpm(220.0))

	val curveRaster		= 256

	val dataBase		= AppDirs forApp "jackdaw"

	// at least the number of decks
	val minCacheCount	= 3

	val maxCacheSize	= 10 * 1024L * 1024L * 1024L	// 10 gig
}
