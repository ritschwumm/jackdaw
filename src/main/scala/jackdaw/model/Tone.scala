package jackdaw.model

import scutil.base.implicits._
import scutil.lang._

import screact._

import jackdaw.range._

object Tone {
	val trimStep:Boolean=>Double	=
			_ cata (TrimRange.size/20, TrimRange.size/100)
		
	val filterStep:Boolean=>Double	=
			_ cata (FilterRange.size/100, FilterRange.size/250)
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
	
	def moveTrim(steps:Int, fine:Boolean) {
		trim	modify trimModifier(steps, fine)
	}
	
	def moveFilter(steps:Int, fine:Boolean) {
		filter	modify filterModifier(steps, fine)
	}
	
	def moveLow(steps:Int, fine:Boolean) {
		low		modify trimModifier(steps, fine)
	}
	
	def moveMiddle(steps:Int, fine:Boolean) {
		middle	modify trimModifier(steps, fine)
	}
	
	def moveHigh(steps:Int, fine:Boolean) {
		high	modify trimModifier(steps, fine)
	}
	
	def resetTrim() {
		trim	set TrimRange.neutral
	}
	
	def resetFilter() {
		filter	set FilterRange.neutral
	}
	
	def resetLow() {
		low		set TrimRange.neutral
	}
	
	def resetMiddle() {
		middle	set TrimRange.neutral
	}
	
	def resetHigh() {
		high	set TrimRange.neutral
	}
	
	def resetAll() {
		resetTrim()
		resetFilter()
		resetLow()
		resetMiddle()
		resetHigh()
	}
	
	private def trimModifier(steps:Int, fine:Boolean):Endo[Double]	=
			it => TrimRange clamp (it + steps * (Tone trimStep fine))
		
	private def filterModifier(steps:Int, fine:Boolean):Endo[Double]	=
			it => FilterRange clamp (it + steps * (Tone filterStep fine))
}
