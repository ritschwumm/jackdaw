package djane.player

object EngineFeedback {
	val empty	= EngineFeedback(
		masterPeak	= 0,
		player1		= PlayerFeedback.empty,
		player2		= PlayerFeedback.empty,
		player3		= PlayerFeedback.empty
	)
}

case class EngineFeedback(
	masterPeak:Float,
	player1:PlayerFeedback,
	player2:PlayerFeedback,
	player3:PlayerFeedback
)
