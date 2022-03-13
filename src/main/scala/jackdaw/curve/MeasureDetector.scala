package jackdaw.curve

import scala.math.*

object MeasureDetector {
	/*
	def main(args:Array[String]) {
		import java.io.File
		import scutil.Implicits.*

		if (args.length != 1) {
			System.err println("usage: MeasureDetector file.wav")
			System exit 1
		}
		val file	= new File(args(0))
		// BETTER handle exceptions
		val sample	= Wav load file
		val curve	= BandCurve calculate (sample, 256)

		val beatsPerMs		= 4
		val framesPerMs		= measureFrames(curve, (bpm(60.0), bpm(200.0), beatsPerMs)

		val fpm		= fragmentsPerMeasure(curve.fragmentRate, beatsPerMs) _
		val bpm		= fpm(framesPerMs/curve.rasterFrames)

		println("framesPerMs=" + framesPerMs)
		println("bpm=" + bpm)
	}
	*/

	//------------------------------------------------------------------------------

	def measureFrames(curve:BandCurve, bpsRange:(Double,Double), measureBeats:Double):Double	= {
		val fpmRange	= measureFrameCandidates(bpsRange, measureBeats, curve.fragmentRate)
		val	best		= bestMeasureFragments(curve.valuesFull.toIndexedSeq, fpmRange)
		// val bpm	= fpm(best)
		best*curve.rasterFrames
	}

	private def measureFrameCandidates(bpsRange:(Double,Double), measureBeats:Double, fragmentRate:Double):(Int,Int) = {
		val fpm	= fragmentsPerMeasure(fragmentRate, measureBeats) _
		(
			floor(fpm(bpsRange._2)).toInt,
			ceil(fpm(bpsRange._1)).toInt
		)
	}

	private def bestMeasureFragments(values:IndexedSeq[Float], fpmRange:(Int,Int)):Int =
		(fpmRange._1 until fpmRange._2)
		.map { fpm => (fpm, differences(values, fpm)) }
		.sortWith { _._2 < _._2 }
		.apply(0)._1

	private def differences(values:IndexedSeq[Float], measureFragments:Int):Float = {
		var	i	= 0
		var d	= 0f
		while (i+measureFragments < values.size) {
			d	+= abs(values(i) - values(i+measureFragments)).toFloat
			i	+= 1
		}
		d / i
	}

	private def fragmentsPerMeasure(fragmentRate:Double, measureBeats:Double)(beatsPerSecond:Double):Double =
		fragmentRate * measureBeats / beatsPerSecond
}
