package jackdaw.player

/** changes to an Engine's state */
sealed abstract class EngineAction

case class EngineChangeControl(speaker:Double, phone:Double)	extends EngineAction
case class EngineSetBeatRate(beatRate:Double) 					extends EngineAction
case class EngineControlPlayer(player:Int, action:PlayerAction)	extends EngineAction
