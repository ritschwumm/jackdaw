package jackdaw.data

object LoopDef {
	val all	=
		Seq(
			LoopDef(1),
			LoopDef(2),
			LoopDef(4),
			LoopDef(8)
		)
}

final case class LoopDef private (measures:Int) {
	def size:RhythmValue	= RhythmValue(measures, RhythmUnit.Measure)
}
