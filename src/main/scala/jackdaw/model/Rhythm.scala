package jackdaw.model

import scala.math._
import scala.collection.mutable.ArrayBuffer

import scutil.lang.ISeq
import scutil.implicits._
import scutil.math._

import jackdaw.audio.PitchMath._

object Rhythm {
	// TODO hardcoded, look up references
	val defaultBeatsPerMeasure	= 4
	val defaultBeatsPerSecond	= bpm(120.0)
	 
	private val defaultMeasureRate	= defaultBeatsPerSecond / defaultBeatsPerMeasure
	
	def simple(anchor:Double, sampleRate:Double):Rhythm	=
			Rhythm(
					anchor,
					defaultMeasure(sampleRate),
					defaultBeatsPerMeasure)
	
	private def defaultMeasure(sampleRate:Double):Double	=
			sampleRate/defaultMeasureRate
}

// BETTER use BigFraction here?
case class Rhythm(anchor:Double, measure:Double, beatsPerMeasure:Int) {
	val beat	= measure / beatsPerMeasure
	
	def moveBy(offset:Double):Rhythm	= copy(anchor	= anchor	+ offset)
	def resizeBy(factor:Double):Rhythm	= copy(measure	= measure	* factor)
	
	/** the raster tick under the cursor shall move with the same speed, independent of the distance from the raster's offset */
	def resizeAt(position:Double, offset:Double):Rhythm = {
		// if within the first beat treat it as at the first beat
		val raw			= position - anchor
		val dir			= signum(raw)
		val distance	= max(abs(raw), beat) * (dir == 0.0 cata (dir, 1.0))
		val	change		= offset / distance
		val changed		= measure * (change + 1)
		// don't let the size come near zero
		if (changed >= abs(offset))	copy(measure=changed)
		else						this
	}

	//------------------------------------------------------------------------------

	def raster(rhythmUnit:RhythmUnit):Raster	= rhythmUnit match {
		case Measure	=> measureRaster
		case Beat		=> beatRaster
	}
	
	lazy val measureRaster:Raster	= Raster(measure,	anchor)
	lazy val beatRaster:Raster		= Raster(beat,		anchor)
	
	//------------------------------------------------------------------------------
	
	def index(position:Double):RhythmIndex	= {
		val rawBeat:Int		= fixFloor(beatRaster normalize position)
		val outBeat:Int		= moduloInt(rawBeat, beatsPerMeasure)
		val outMeasure:Int	= fixFloor(measureRaster normalize position)
		RhythmIndex(outMeasure, outBeat)
	}
	
	/** positive distance */
	def measureDistance(offset:Double):Int	=
			fixCeil(offset / measure)
	
	private val small	= {
		val	epsilon	= 1.0/1000
		Raster(epsilon, 0)
	}
	
	/** floor ignoring small errors */
	private def fixFloor(value:Double):Int	=
			floor(small round value).toInt
		
	/** ceil ignoring small errors */
	private def fixCeil(value:Double):Int	=
			ceil(small round value).toInt
	
	//------------------------------------------------------------------------------
	
	def lines(start:Double, end:Double):ISeq[RhythmLine] = {
		val	firstValue	= beatRaster ceil start
		val firstIndex	= rint((firstValue - anchor) / beat).toInt
		
		val out		= new ArrayBuffer[RhythmLine]
		var index	= firstIndex
		var value	= firstValue
		while (value < end) {
			out		+= (
					 if (index == 0)					AnchorLine(value)
				else if (index % beatsPerMeasure == 0)	MeasureLine(value)
				else									BeatLine(value)
			)
			value	+= beat
			index	+= 1
		}
		
		out.toVector
	}
}
