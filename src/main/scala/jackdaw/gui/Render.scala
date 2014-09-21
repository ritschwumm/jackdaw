package jackdaw.gui

import java.util.Locale

import scala.math._

import scutil.implicits._

import scaudio.math._

import jackdaw.model._

object Render {
	def rhythmIndexOpt(it:Option[RhythmIndex]):String	=
			it cata ("-/-", rhythmIndex)
		
	def rhythmIndex(it:RhythmIndex):String	=
			octal(it.measure) + "/" + octal(it.beat)
		
	def beatsOpt(it:Option[Int]):String	=
			it cata ("-", beats)
		
	def beats(it:Int):String	=
			octal(it)
		
	private def octal(it:Int)	= 
			Integer toString (it, 8)
	
	def bpmOpt(hertz:Option[Double]):String =
			hertz cata ("---.--", bpm)
			
	def bpm(hertz:Double):String	= 
			"%.2f" formatLocal (Locale.US, hertz * secondsPerMinute)
	
	/*
	def secondsOpt(it:Option[Double]):String	=
			it cata (seconds, "-:--:--")
	
	def seconds(it:Double):String	=
			{
				val	rounded	= round(it).toInt
				val seconds	= rounded % secondsPerMinute
				val minutes	= rounded / secondsPerMinute
				"%d:%02d" format (minutes, seconds)
			}
	*/
	
	private val centsEpsilon	= 0.05
	
	def cents(octave:Double):String = {
		val cents	= octave * 12 * 100
		if (abs(cents) >= centsEpsilon)	"%+.1f" formatLocal (Locale.US, cents)
		else							"0"
	}
}
