package jackdaw.data

import scutil.time._

final case class Stamped[T](stamp:MilliInstant, data:T) {
	def map[U](func:T=>U):Stamped[U]	= Stamped(stamp, func(data))
}
