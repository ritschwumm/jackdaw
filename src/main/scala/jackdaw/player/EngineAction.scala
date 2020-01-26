package jackdaw.player

object EngineAction {
	final case class ChangeControl(speaker:Double, phone:Double)	extends EngineAction
	final case class SetBeatRate(beatRate:Double) 					extends EngineAction
	final case class ControlPlayer(player:Int, action:PlayerAction)	extends EngineAction
}

/** changes to an Engine's state */
sealed abstract class EngineAction
