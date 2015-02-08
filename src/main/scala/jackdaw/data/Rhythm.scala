package jackdaw.data

import scala.math._
import scala.collection.mutable

import scutil.lang.ISeq
import scutil.implicits._
import scutil.math._

import jackdaw.audio.PitchMath._

object Rhythm {
	def default(anchor:Double, measure:Double):Rhythm	=
			Rhythm(
				anchor		= anchor,
				measure		= measure,
				schema		= Schema.default
			)
			
	val defaultBeatsPerSecond	= bpm(120.0)
	
	def fake(anchor:Double, sampleRate:Double):Rhythm	=
			default(
				anchor	= anchor,
				measure	= sampleRate * Schema.default.beatsPerMeasure / defaultBeatsPerSecond
			)
}

// BETTER use BigFraction here?
case class Rhythm(anchor:Double, measure:Double, schema:Schema) {
	val beat	= measure / schema.beatsPerMeasure
	val phrase	= measure * schema.measuresPerPhrase
	
	//------------------------------------------------------------------------------
	
	def withAnchor(anchor:Double):Rhythm	= copy(anchor = anchor)
	
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

	def raster(rhythmUnit:RhythmUnit):Raster	=
			rhythmUnit match {
				case Phrase		=> phraseRaster
				case Measure	=> measureRaster
				case Beat		=> beatRaster
			}
	
	lazy val phraseRaster:Raster	= Raster(phrase,	anchor)
	lazy val measureRaster:Raster	= Raster(measure,	anchor)
	lazy val beatRaster:Raster		= Raster(beat,		anchor)
	
	//------------------------------------------------------------------------------
	
	def index(position:Double):RhythmIndex	=
			RhythmIndex(
				beat	= moduloInt(fixFloor(beatRaster		normalize position),	schema.beatsPerMeasure),
				measure	= moduloInt(fixFloor(measureRaster	normalize position),	schema.measuresPerPhrase),
				phrase	= 			fixFloor(phraseRaster	normalize position)
			)
	
	private val small	= {
		val	epsilon	= 1.0/1000
		Raster(epsilon, 0)
	}
	
	/** floor ignoring small errors */
	private def fixFloor(value:Double):Int	=
			floor(small round value).toInt
		
	//------------------------------------------------------------------------------
	
	def lines(start:Double, end:Double):ISeq[RhythmLine] = {
		val	firstValue	= beatRaster ceil start
		val firstIndex	= rint((firstValue - anchor) / beat).toInt
		
		val out		= new mutable.ArrayBuffer[RhythmLine]
		var index	= firstIndex
		var value	= firstValue
		while (value < end) {
			out		+= (
				// if (index == 0)	AnchorLine(value)
				     if (index % schema.beatsPerPhrase  == 0)	RhythmLine(value, Phrase)
				else if (index % schema.beatsPerMeasure == 0)	RhythmLine(value, Measure)
				else											RhythmLine(value, Beat)
			)
			value	+= beat
			index	+= 1
		}
		
		out.toVector
	}
}
