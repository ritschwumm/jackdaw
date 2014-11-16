package jackdaw.model

import scutil.lang._
import scutil.implicits._

import screact._

import scaudio.math._

import jackdaw.audio._

object Strip {
	def forMaster	= new Strip(VolumeRange.alot,	VolumeRange.alot,	MasterRange)
	def forDeck		= new Strip(VolumeRange.max,	VolumeRange.min,	DeckRange)
	
	val step:Boolean=>Double	= _ cata (VolumeRange.size/40, VolumeRange.size/100)
	
	val strip2gain:Endo[Double]	= gammaFade(+0.66)
}

/** distribute signal to speaker and master */
final class Strip(initialSpeaker:Double, initialPhone:Double, val meterRange:MeterRange) {
	val	speaker	= cell(initialSpeaker)	// VolumeRange	
	val	phone	= cell(initialPhone)	// VolumeRange
	
	val speakerGain:Signal[Double]	= speaker	map	Strip.strip2gain
	val phoneGain:Signal[Double]	= phone		map	Strip.strip2gain
	
	def moveSpeaker(steps:Int, fine:Boolean) {
		speaker modify modifier(steps, fine)
	}
	
	def movePhone(steps:Int, fine:Boolean) {
		phone modify modifier(steps, fine)
	}
	
	private def modifier(steps:Int, fine:Boolean):Endo[Double]	=
			it => VolumeRange clamp (it + steps * (Strip step fine)) 
}
