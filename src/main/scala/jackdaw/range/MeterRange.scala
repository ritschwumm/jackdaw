package jackdaw.range

import scaudio.math._

trait MeterRange {
	val over:Double
	val warn:Double
	val ok:Double
	val zero:Double
}

object DeckRange extends MeterRange {
	val over	= db2gain(+6)
	val warn	= unitGain
	val ok		= db2gain(-3)
	val zero	= zeroGain
}

object MasterRange extends MeterRange {
	val over	= unitGain
	val warn	= db2gain(-3)
	val ok		= db2gain(-6)
	val zero	= zeroGain
}
