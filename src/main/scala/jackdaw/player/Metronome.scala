package jackdaw.player

import scutil.lang._

import screact._

import jackdaw.model._

trait MetronomeContext {
	def beatRateChanged(beatRate:Double):Unit
	def running:Boolean
}

object Metronome {
	val beatsPerMeasure	= Rhythm.defaultBeatsPerMeasure
}

/** 
something a Player can be synced to
public methods must never be called outside the engine thread
*/
final class Metronome(outputRate:Double, ctx:MetronomeContext) {
	private var bRate		= 1.0
	private var mIncrement	= 0.0
	private var mPhase		= 0.0
	private def bPhase		= mPhase * Metronome.beatsPerMeasure % 1.0
	
	private[player] def setBeatRate(beatRate:Double) {
		bRate		= beatRate
		val mRate	= bRate / Metronome.beatsPerMeasure
		mIncrement	= mRate / outputRate
		
		ctx beatRateChanged beatRate
	} 
	
	private[player] def step() {
		if (ctx.running)	mPhase	= (mPhase + mIncrement) % 1.0
		else				mPhase	= 0
	}
	
	//------------------------------------------------------------------------------
	
	private[player] def phase(rhythmUnit:RhythmUnit):Double	=
			rhythmUnit match {
				case Measure	=> mPhase
				case Beat		=> bPhase
			}
	
	private[player] def	beatRate:Double	= bRate
}
