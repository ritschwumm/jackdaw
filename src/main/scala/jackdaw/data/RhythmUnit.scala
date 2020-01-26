package jackdaw.data

object RhythmUnit {
	case object Phrase	extends RhythmUnit
	case object Measure	extends RhythmUnit
	case object Beat	extends RhythmUnit
}

sealed abstract class RhythmUnit

