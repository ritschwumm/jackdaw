package jackdaw.model

import scutil.time._

case class Stamped[T](stamp:MilliInstant, data:T)