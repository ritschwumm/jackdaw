package jackdaw.data

import java.lang.{ Math as JMath }

final case class Raster(size:Double, offset:Double) {
	def floor(it:Double):Double	= denormalize(JMath.floor(normalize(it)))
	def round(it:Double):Double	= denormalize(JMath.rint(normalize(it)))
	def ceil(it:Double):Double	= denormalize(JMath.ceil(normalize(it)))

	def normalize(it:Double):Double		= (it - offset) / size
	def denormalize(it:Double):Double	= it * size + offset

	def phase(it:Double):Double	= normalize(it) % 1.0
}
