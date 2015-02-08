package jackdaw.gui

import java.util.Locale

import scala.math._

import scutil.implicits._

import scaudio.math._

import jackdaw.data._

object Render {
	def rhythmIndexOpt(it:Option[RhythmIndex]):String	=
			it cata ("-:-:-", rhythmIndex)
		
	def rhythmIndex(it:RhythmIndex):String	=
			decimal(it.phrase) + ":" + decimal(it.measure) + ":" + decimal(it.beat)
		
	private def decimal(it:Int)	=
			Integer toString (it, 10)
	
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
