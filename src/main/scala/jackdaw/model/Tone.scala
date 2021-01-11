package jackdaw.model

import scutil.core.implicits._

import screact._

import jackdaw.range._

object Tone {
	val trimStep:Boolean=>Double	=
		_.cata(TrimRange.size/20, TrimRange.size/100)

	val filterStep:Boolean=>Double	=
		_.cata(FilterRange.size/100, FilterRange.size/250)
}

final class Tone {
	// fader values
	val trim	= cell(TrimRange.neutral)
	val filter	= cell(FilterRange.neutral)
	val	low		= cell(TrimRange.neutral)
	val	middle	= cell(TrimRange.neutral)
	val	high	= cell(TrimRange.neutral)

	// gain values
	val trimGain:Signal[Double]		= trim
	val filterValue:Signal[Double]	= filter
	val	lowGain:Signal[Double]		= low
	val	middleGain:Signal[Double]	= middle
	val	highGain:Signal[Double]		= high

	def moveTrim(steps:Int, fine:Boolean):Unit	= {
		trim	modify trimModifier(steps, fine)
	}

	def moveFilter(steps:Int, fine:Boolean):Unit	= {
		filter	modify filterModifier(steps, fine)
	}

	def moveLow(steps:Int, fine:Boolean):Unit	= {
		low		modify trimModifier(steps, fine)
	}

	def moveMiddle(steps:Int, fine:Boolean):Unit	= {
		middle	modify trimModifier(steps, fine)
	}

	def moveHigh(steps:Int, fine:Boolean):Unit	= {
		high	modify trimModifier(steps, fine)
	}

	def resetTrim():Unit	= {
		trim	set TrimRange.neutral
	}

	def resetFilter():Unit	= {
		filter	set FilterRange.neutral
	}

	def resetLow():Unit	= {
		low		set TrimRange.neutral
	}

	def resetMiddle():Unit	= {
		middle	set TrimRange.neutral
	}

	def resetHigh():Unit	= {
		high	set TrimRange.neutral
	}

	def resetAll():Unit	= {
		resetTrim()
		resetFilter()
		resetLow()
		resetMiddle()
		resetHigh()
	}

	private def trimModifier(steps:Int, fine:Boolean):Double=>Double	=
		it => TrimRange clamp (it + steps * (Tone trimStep fine))

	private def filterModifier(steps:Int, fine:Boolean):Double=>Double	=
		it => FilterRange clamp (it + steps * (Tone filterStep fine))
}
