package jackdaw.player

import scaudio.sample.Sample

import jackdaw.model._

/** changes to a Player's state */
object PlayerAction {
	sealed abstract class Running extends PlayerAction
	case object RunningOn	extends Running
	case object RunningOff	extends Running
	
	sealed abstract class Pitch extends PlayerAction
	case class PitchAbsolute(pitch:Double)	extends Pitch
	
	sealed abstract class NeedsFading extends PlayerAction
	
	sealed abstract class Phase	extends NeedsFading
	case class  PhaseAbsolute(rhythmUnit:RhythmUnit, offset:Double)	extends Phase
	case class  PhaseRelative(rhythmUnit:RhythmUnit, offset:Double)	extends Phase
	
	sealed abstract class Position extends NeedsFading
	case class PositionAbsolute(frame:Double)						extends Position
	case class PositionJump(frame:Double, rhythmUnit:RhythmUnit)	extends Position
	case class PositionSeek(steps:Double, rhythmUnit:RhythmUnit)	extends Position
	
	sealed abstract class Scratch extends PlayerAction
	case object ScratchBegin					extends Scratch
	case object ScratchEnd						extends Scratch
	case class  ScratchRelative(frames:Double)	extends Scratch
	
	sealed abstract class Drag extends PlayerAction
	case object DragBegin				extends Drag
	case object DragEnd					extends Drag
	case class  DragAbsolute(v:Double)	extends Drag
	
	sealed abstract class Control	extends PlayerAction
	case class SetNeedSync(needSync:Boolean)		extends Control
	case class ChangeControl(
		trim:Double,
		filter:Double,
		low:Double,
		middle:Double,
		high:Double,
		speaker:Double,
		phone:Double,
		sample:Option[Sample],
		rhythm:Option[Rhythm]
	) 
	extends Control
}

sealed abstract class PlayerAction
