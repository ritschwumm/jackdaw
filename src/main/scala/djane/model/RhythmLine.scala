package djane.model

object RhythmLine {
	case class AnchorLine(frame:Double)		extends RhythmLine
	case class MeasureLine(frame:Double)	extends RhythmLine
	case class BeatLine(frame:Double)		extends RhythmLine
}

sealed abstract class RhythmLine {
	val frame:Double
}
