package jackdaw.persistence

import java.nio.file.*
import java.io.*

import scutil.core.implicits.*
import scutil.jdk.implicits.*
import scutil.log.*

import jackdaw.curve.BandCurve

/** loading and saving an BandCurvePersister from/to a File */
final class BandCurvePersister extends Persister[BandCurve] with Logging {
	def load(file:Path):Option[BandCurve] = {
		try {
			new ObjectInputStream(Files.newInputStream(file)) use { in =>
				val	fragmentRate	= in.readDouble
				val	rasterFrames	= in.readInt
				val	chunkCount		= in.readInt
				val	valuesFull		= readFloatArray(in, chunkCount)
				val	valuesLow		= readFloatArray(in, chunkCount)
				val	valuesMiddle	= readFloatArray(in, chunkCount)
				val	valuesHigh		= readFloatArray(in, chunkCount)
				Some(BandCurve(
					fragmentRate, rasterFrames, chunkCount,
					valuesFull, valuesLow, valuesMiddle, valuesHigh
				))
			}
		}
		catch { case e:Exception	=>
			ERROR("cannot unmarshall file", file, e)
			None
		}
	}

	def save(file:Path)(curve:BandCurve):Unit	= {
		try {
			new ObjectOutputStream(Files.newOutputStream(file)) use { out =>
				out writeDouble	curve.fragmentRate
				out writeInt	curve.rasterFrames
				out writeInt	curve.chunkCount
				writeFloatArray(out, curve.valuesFull)
				writeFloatArray(out, curve.valuesLow)
				writeFloatArray(out, curve.valuesMiddle)
				writeFloatArray(out, curve.valuesHigh)
			}
		}
		catch { case e:Exception	=>
			ERROR("cannot marshall file", file, e)
		}
	}

	private def readFloatArray(in:ObjectInputStream, size:Int):Array[Float] = {
		val array	= Array.ofDim[Float](size)
		var i	= 0
		while (i < size) {
			array(i)	= in.readFloat
			i	= i + 1
		}
		array
	}

	private def writeFloatArray(out:ObjectOutputStream, array:Array[Float]):Unit	= {
		var i = 0
		while (i < array.length) {
			out writeFloat array(i)
			i	= i + 1
		}
	}
}
