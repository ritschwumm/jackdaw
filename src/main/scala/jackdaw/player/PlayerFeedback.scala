package jackdaw.player

import jackdaw.data.*

object PlayerFeedback {
	val empty	=
		PlayerFeedback(
			running			= false,
			afterEnd		= false,
			position		= 0,
			pitch			= 0.0,
			measureMatch	= None,
			beatRate		= None,
			needSync		= true,
			hasSync			= false,
			masterPeak		= 0,
			loopSpan		= None,
			loopDef			= None
		)
}

final case class PlayerFeedback(
	running:Boolean,
	afterEnd:Boolean,
	position:Double,
	pitch:Double,
	measureMatch:Option[Double],
	beatRate:Option[Double],
	needSync:Boolean,
	hasSync:Boolean,
	masterPeak:Float,
	loopSpan:Option[Span],
	loopDef:Option[LoopDef]
)
