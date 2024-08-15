package jackdaw.curve

import scala.math.*

import scutil.math.functions.*

import scaudio.sample.Sample
import scaudio.dsp.Equalizer

import jackdaw.Config

object BandCurve {
	def calculate(sample:Sample, rasterFrames:Int):BandCurve	= {
		val fragmentRate	= sample.frameRate / rasterFrames

		val chunkCount		= (sample.frameCount + rasterFrames - 1) / rasterFrames
		val equalizer		= new Equalizer(Config.lowEq, Config.highEq, sample.frameRate)
		val valuesFull		= new Array[Float](chunkCount)
		val valuesLow		= new Array[Float](chunkCount)
		val valuesMiddle	= new Array[Float](chunkCount)
		val valuesHigh		= new Array[Float](chunkCount)

		var	frame			= 0
		var chunk			= 0
		var energyFull		= 0.0
		var energyLow		= 0.0
		var energyMiddle	= 0.0
		var energyHigh		= 0.0
		var summed	= 0

		while (frame < sample.frameCount) {
			var	channel	= 0
			var mono	= 0.0
			while (channel < sample.channels.size) {
				mono	+= sample.channels(channel).get(frame)
				channel	+= 1
			}
			mono	/= sample.channels.size

			// val (valueLow, valueMiddle, valueHigh)	= equalizer nextBands mono
			equalizer.step(mono)
			val valueLow	= equalizer.l
			val valueMiddle	= equalizer.m
			val valueHigh	= equalizer.h

			energyFull		+= abs(mono)
			energyLow		+= abs(valueLow)
			energyMiddle	+= abs(valueMiddle)
			energyHigh		+= abs(valueHigh)
			frame	+= 1
			summed	+= 1

			if (frame % rasterFrames == 0 || frame == sample.frameCount) {
				valuesFull(chunk)	= energyFull.toFloat	/ summed
				valuesLow(chunk)	= energyLow.toFloat		/ summed
				valuesMiddle(chunk)	= energyMiddle.toFloat	/ summed
				valuesHigh(chunk)	= energyHigh.toFloat	/ summed
				energyFull		= 0
				energyLow		= 0
				energyMiddle	= 0
				energyHigh		= 0
				summed	= 0
				chunk	+= 1
			}
		}

		BandCurve(
			fragmentRate,
			rasterFrames,
			chunkCount,
			valuesFull,
			valuesLow,
			valuesMiddle,
			valuesHigh
		)
	}
}

final case class BandCurve(
	fragmentRate:Double,
	rasterFrames:Int,
	chunkCount:Int,
	valuesFull:Array[Float],
	valuesLow:Array[Float],
	valuesMiddle:Array[Float],
	valuesHigh:Array[Float]
) {
	@inline def rangeFull(start:Int, size:Int):Float	= rangeImpl(valuesFull,		start, size)
	@inline def rangeLow(start:Int, size:Int):Float		= rangeImpl(valuesLow,		start, size)
	@inline def rangeMiddle(start:Int, size:Int):Float	= rangeImpl(valuesMiddle,	start, size)
	@inline def rangeHigh(start:Int, size:Int):Float	= rangeImpl(valuesHigh,		start, size)

	def rangeImpl(values:Array[Float], start:Int, size:Int):Float = {
		val	end		= start + size
		val	startR	= clampInt(start / rasterFrames, 0, chunkCount-1)
		val endR	= clampInt(end   / rasterFrames, 0, chunkCount-1)

		val sizeR	= endR - startR
		if (sizeR <= 0)	return 0
		if (sizeR == 1)	return values(startR)

		var energyV		= 0f
		var	currentR	= startR
		while (currentR < endR) {
			energyV		+= values(currentR)
			currentR	+= 1
		}
		energyV	/ sizeR
	}

	// NOTE this might be up to rasterFrames-1 more than the sample actually contains
	val frameCount		= chunkCount * rasterFrames
}
