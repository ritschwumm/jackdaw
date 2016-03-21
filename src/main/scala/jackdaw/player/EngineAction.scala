package jackdaw.player

/** changes to an Engine's state */
sealed abstract class EngineAction

final case class EngineChangeControl(speaker:Double, phone:Double)	extends EngineAction
final case class EngineSetBeatRate(beatRate:Double) 					extends EngineAction
final case class EngineControlPlayer(player:Int, action:PlayerAction)	extends EngineAction
