package jackdaw.persistence

import java.io._

import scutil.implicits._
import scutil.log._

import jackdaw.audio.BandCurve

/** loading and saving an BandCurvePersister from/to a File */
final class BandCurvePersister extends Persister[BandCurve] with Logging {
	def load(file:File):Option[BandCurve] = {
		try {
			new ObjectInputStream(new FileInputStream(file)) use { in =>
				val	sampleRate		= in.readDouble
				val	rasterFrames	= in.readInt
				val	chunkCount		= in.readInt
				val	valuesFull		= in.readObject.asInstanceOf[Array[Float]]
				val	valuesLow		= in.readObject.asInstanceOf[Array[Float]]
				val	valuesMiddle	= in.readObject.asInstanceOf[Array[Float]]
				val	valuesHigh		= in.readObject.asInstanceOf[Array[Float]]
				Some(BandCurve(
						sampleRate, rasterFrames, chunkCount, 
						valuesFull, valuesLow, valuesMiddle, valuesHigh))
			}
		}
		catch { case e:Exception	=>
			ERROR("cannot unmarshall file: " + file, e)
			None
		}
	}
	
	def save(file:File)(curve:BandCurve) {
		try {
			new ObjectOutputStream(new FileOutputStream(file)) use { out =>
				out writeDouble	curve.sampleRate
				out writeInt	curve.rasterFrames
				out writeInt	curve.chunkCount
				out writeObject	curve.valuesFull
				out writeObject	curve.valuesLow
				out writeObject	curve.valuesMiddle
				out writeObject	curve.valuesHigh
			}
		}
		catch { case e:Exception	=>
			ERROR("cannot marshall file: " + file, e)
		}
	}
}
