package jackdaw.model

import scutil.lang.ISeq

object LoopDef {
	val all	=
			ISeq(
				LoopDef(1),
				LoopDef(2),
				LoopDef(4),
				LoopDef(8)
			)
}

final case class LoopDef private (measures:Int) {
	def size:RhythmValue	= RhythmValue(measures, Measure)
}
