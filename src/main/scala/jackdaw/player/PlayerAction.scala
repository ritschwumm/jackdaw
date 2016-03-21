package jackdaw.player

import java.io.File

import jackdaw.data._

/** changes to a Player's state */
sealed abstract class PlayerAction

final case class PlayerChangeControl(
	trim:Double,
	filter:Double,
	low:Double,
	middle:Double,
	high:Double,
	speaker:Double,
	phone:Double
)
extends PlayerAction

final case class PlayerSetNeedSync(needSync:Boolean)	extends PlayerAction

final case class PlayerSetFile(file:Option[File])			extends PlayerAction
final case class PlayerSetRhythm(rhythm:Option[Rhythm])	extends PlayerAction

final case class PlayerSetRunning(running:Boolean)	extends PlayerAction

final case class PlayerPitchAbsolute(pitch:Double, keepSync:Boolean)	extends PlayerAction

final case class PlayerPhaseAbsolute(position:RhythmValue)	extends PlayerAction
final case class PlayerPhaseRelative(offset:RhythmValue)		extends PlayerAction

final case class PlayerPositionAbsolute(frame:Double)						extends PlayerAction
// TODO raster could be a RhythmValue - when there's no rhythm, use PlayerPositionAbsolute
// TODO wrong: the RhythmValue here is used as a raster, but the current phase is kept stable.. untangle this.
final case class PlayerPositionJump(frame:Double, rhythmUnit:RhythmUnit)	extends PlayerAction
final case class PlayerPositionSeek(offset:RhythmValue)					extends PlayerAction
	
final case class  PlayerDragAbsolute(v:Double)	extends PlayerAction
case object PlayerDragEnd					extends PlayerAction
	
final case class  PlayerScratchRelative(frames:Double)	extends PlayerAction
case object PlayerScratchEnd						extends PlayerAction

final case class PlayerLoopEnable(preset:LoopDef)	extends PlayerAction
case object PlayerLoopDisable				extends PlayerAction
