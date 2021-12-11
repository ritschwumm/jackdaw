package jackdaw.data

object LoopDef {
	val all	=
		Seq(
			LoopDef(1),
			LoopDef(2),
			LoopDef(4),
			LoopDef(8)
		)

	// NOTE this should _not_ be used for anything else than deserialization in remote.Input
	def fromInput(measures:Int):LoopDef	= new LoopDef(measures)
}

final case class LoopDef private (measures:Int) {
	def size:RhythmValue	= RhythmValue(measures, RhythmUnit.Measure)
}
