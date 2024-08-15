package jackdaw.player

/** changes to an Engine's state */
enum EngineAction {
	case ChangeControl(speaker:Double, phone:Double)
	case SetBeatRate(beatRate:Double)
	case ControlPlayer(player:Int, action:PlayerAction)
}
