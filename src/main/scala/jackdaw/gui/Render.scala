package jackdaw.gui

import java.util.Locale

import scala.math.*

import scutil.core.implicits.*
import scutil.math.functions.*

import scaudio.math.*

import jackdaw.data.*
import jackdaw.key.*

object Render {
	def rhythmIndexOpt(it:Option[RhythmIndex]):String	=
		it.cata("-:-:-", rhythmIndex)

	private def rhythmIndex(it:RhythmIndex):String	=
		decimal(it.phrase) + ":" + decimal(it.measure) + ":" + decimal(it.beat)

	private def decimal(it:Int)	=
		Integer.toString(it, 10)

	def bpmOpt(hertz:Option[Double]):String =
		hertz.cata("---.--", bpm)

	def bpm(hertz:Double):String	=
		"%.2f".formatLocal(Locale.US, hertz * secondsPerMinute)

	def effectiveKeyOpt(it:Option[Option[DetunedChord]]):String	=
		it.cata("--", effectiveKey)

	private def effectiveKey(it:Option[DetunedChord]):String	=
		it.cata("~~", effectiveChord)

	private def effectiveChord(it:DetunedChord):String	=
		chord(it.chord) + " " + detune(it.detune)

	private def chord(mk:MusicChord):String	=
		basicCof(mk.root.index, mk.scale.cata (3, 0)).toString +
		" " +
		notes(mk.root.index) + mk.scale.cata ("", "m")

	private def basicCof(pitch:Int, offset:Int):Int	=
		moduloInt(pitch * 7 + offset, 12) + 1

	private val notes	=
		Vector(
			"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"
		)

	private def detune(it:Detune):String	=
		it match {
			case Detune.VeryHigh	=> "⇈"
			case Detune.High		=> "↑"
			case Detune.InTune		=> ""
			case Detune.Low		=> "↓"
			case Detune.VeryLow	=> "⇊"
		}
	private val centsEpsilon	= 0.05

	def cents(octave:Double):String = {
		val cents	= octave * 12 * 100
		if (abs(cents) >= centsEpsilon)	"%+.1f".formatLocal(Locale.US, cents)
		else							"0"
	}

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
}
