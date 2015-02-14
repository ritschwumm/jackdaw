package jackdaw.player

import java.io.File

import jackdaw.data._

/** changes to a Player's state */
sealed abstract class PlayerAction

case class PlayerChangeControl(
	trim:Double,
	filter:Double,
	low:Double,
	middle:Double,
	high:Double,
	speaker:Double,
	phone:Double
)
extends PlayerAction

case class PlayerSetNeedSync(needSync:Boolean)	extends PlayerAction

case class PlayerSetFile(file:Option[File])			extends PlayerAction
case class PlayerSetRhythm(rhythm:Option[Rhythm])	extends PlayerAction

case class PlayerSetRunning(running:Boolean)	extends PlayerAction

case class PlayerPitchAbsolute(pitch:Double, keepSync:Boolean)	extends PlayerAction

case class PlayerPhaseAbsolute(position:RhythmValue)	extends PlayerAction
case class PlayerPhaseRelative(offset:RhythmValue)		extends PlayerAction

case class PlayerPositionAbsolute(frame:Double)						extends PlayerAction
// TODO raster could be a RhythmValue - when there's no rhythm, use PlayerPositionAbsolute
// TODO wrong: the RhythmValue here is used as a raster, but the current phase is kept stable.. untangle this.
case class PlayerPositionJump(frame:Double, rhythmUnit:RhythmUnit)	extends PlayerAction
case class PlayerPositionSeek(offset:RhythmValue)					extends PlayerAction
	
case class  PlayerDragAbsolute(v:Double)	extends PlayerAction
case object PlayerDragEnd					extends PlayerAction
	
case class  PlayerScratchRelative(frames:Double)	extends PlayerAction
case object PlayerScratchEnd						extends PlayerAction

case class PlayerLoopEnable(preset:LoopDef)	extends PlayerAction
case object PlayerLoopDisable				extends PlayerAction
