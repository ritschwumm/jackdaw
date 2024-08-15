package jackdaw.player

import java.nio.file.Path

import jackdaw.data.*

/** changes to a Player's state */
enum PlayerAction {
	case ChangeControl(
		trim:Double,
		filter:Double,
		low:Double,
		middle:Double,
		high:Double,
		speaker:Double,
		phone:Double
	)

	case SetNeedSync(needSync:Boolean)

	case SetFile(file:Option[Path])
	case SetRhythm(rhythm:Option[Rhythm])

	case SetRunning(running:Boolean)

	case PitchAbsolute(pitch:Double, keepSync:Boolean)

	case PhaseAbsolute(position:RhythmValue)
	case PhaseRelative(offset:RhythmValue)

	case PositionAbsolute(frame:Double)
	// TODO raster could be a RhythmValue - when there's no rhythm, use PlayerPositionAbsolute
	// TODO wrong: the RhythmValue here is used as a raster, but the current phase is kept stable.. untangle this.
	case PositionJump(frame:Double, rhythmUnit:RhythmUnit)
	case PositionSeek(offset:RhythmValue)

	case DragAbsolute(v:Double)
	case DragEnd

	case ScratchRelative(frames:Double)
	case ScratchEnd

	case LoopEnable(preset:LoopDef)
	case LoopDisable
}
