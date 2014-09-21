package jackdaw.player

object PlayerFeedback {
	val empty	= PlayerFeedback(
		running			= false,
		afterEnd		= false,
		position		= 0,
		pitch			= 0.0,
		measureMatch	= None,
		beatRate		= None,
		needSync		= true,
		hasSync			= false,
		masterPeak		= 0
	)
}

case class PlayerFeedback(
	running:Boolean,
	afterEnd:Boolean,
	position:Double,
	pitch:Double,
	measureMatch:Option[Double],
	beatRate:Option[Double],
	needSync:Boolean,
	hasSync:Boolean,
	masterPeak:Float
)
