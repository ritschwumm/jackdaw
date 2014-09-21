package jackdaw.model

object RhythmUnit {
	case object Measure	extends RhythmUnit
	case object Beat	extends RhythmUnit
	// case object Frame extends RhythmUnit
}

sealed abstract class RhythmUnit

