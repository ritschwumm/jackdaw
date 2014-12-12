package jackdaw.player

import scutil.lang._

/** changes to an Engine's state */
object EngineAction {
	case class ChangeControl(
		speaker:Double,
		phone:Double
	)
	extends EngineAction
	
	case class SetBeatRate(beatRate:Double) 							extends EngineAction
	case class ControlPlayer(player:Int, actions:ISeq[PlayerAction])	extends EngineAction
	case class Execute(task:Task)										extends EngineAction
}

sealed abstract class EngineAction
