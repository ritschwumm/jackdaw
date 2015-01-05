package jackdaw.data

sealed abstract class RhythmUnit

case object Measure	extends RhythmUnit
case object Beat	extends RhythmUnit
