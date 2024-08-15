package jackdaw.model

import scutil.core.implicits.*

import screact.*

import scaudio.math.*

import jackdaw.range.*

object Strip {
	def forMaster	= new Strip(VolumeRange.alot,	VolumeRange.alot,	MasterRange)
	def forDeck		= new Strip(VolumeRange.max,	VolumeRange.min,	DeckRange)

	val step:Boolean=>Double	= _.cata(VolumeRange.size/40, VolumeRange.size/100)

	val strip2gain:Double=>Double	= gammaFade(+0.66)
}

/** distribute signal to speaker and master */
final class Strip(initialSpeaker:Double, initialPhone:Double, val meterRange:MeterRange) {
	val	speaker	= cell(initialSpeaker)	// VolumeRange
	val	phone	= cell(initialPhone)	// VolumeRange

	val speakerGain:Signal[Double]	= speaker.signal	.map(Strip.strip2gain)
	val phoneGain:Signal[Double]	= phone.signal		.map(Strip.strip2gain)

	def moveSpeaker(steps:Int, fine:Boolean):Unit	= {
		speaker.modify(modifier(steps, fine))
	}

	def movePhone(steps:Int, fine:Boolean):Unit	= {
		phone.modify(modifier(steps, fine))
	}

	private def modifier(steps:Int, fine:Boolean):Double=>Double	=
		it => VolumeRange.clamp(it + steps * (Strip step fine))
}
