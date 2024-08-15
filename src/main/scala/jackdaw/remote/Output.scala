package jackdaw.remote

import java.io.OutputStream
import java.nio.file.Path

import jackdaw.player.*
import jackdaw.data.*

final class Output(val st:OutputStream) {
	def writeToStub(it:ToStub):Unit	= {
		it match {
		case x@ToStub.Started(_,_)	=> writeByte(0); writeStartedStub(x)
		case x@ToStub.Send(_)		=> writeByte(1); writeSendStub(x)
		}
	}
	def writeStartedStub(it:ToStub.Started):Unit	= {
		writeInt(it.outputRate)
		writeBoolean(it.phoneEnabled)
	}
	def writeSendStub(it:ToStub.Send):Unit	= {
		writeEngineFeedback(it.feedback)
	}
	def writeEngineFeedback(it:EngineFeedback):Unit	= {
		writeFloat(it.masterPeak)
		writePlayerFeedback(it.player1)
		writePlayerFeedback(it.player2)
		writePlayerFeedback(it.player3)
	}
	def writePlayerFeedback(it:PlayerFeedback):Unit	= {
		writeBoolean(it.running)
		writeBoolean(it.afterEnd)
		writeDouble(it.position)
		writeDouble(it.pitch)
		writeOption(it.measureMatch ,writeDouble)
		writeOption(it.beatRate, writeDouble)
		writeBoolean(it.needSync)
		writeBoolean(it.hasSync)
		writeFloat(it.masterPeak)
		writeOption(it.loopSpan, writeSpan)
		writeOption(it.loopDef, writeLoopDef)
	}
	def writeSpan(it:Span):Unit = {
		writeDouble(it.start)
		writeDouble(it.size)
	}
	def writeLoopDef(it:LoopDef):Unit = {
		writeInt(it.measures)
	}

	//------------------------------------------------------------------------------

	def writeToSkeleton(it:ToSkeleton):Unit	= {
		it match {
		case ToSkeleton.Kill		=> writeByte(0); writeToSkeletonKill(ToSkeleton.Kill)
		case x@ToSkeleton.Send(_)	=> writeByte(1); writeToSkeletonSend(x)
		}
	}
	def writeToSkeletonKill(it:ToSkeleton.Kill.type):Unit = {}
	def writeToSkeletonSend(it:ToSkeleton.Send):Unit = {
		writeEngineAction(it.action)
	}
	def writeEngineAction(it:EngineAction):Unit = {
		it match {
		case x@EngineAction.ChangeControl(_,_)	=> writeByte(0); writeEngineAction_ChangeControl(x)
		case x@EngineAction.SetBeatRate(_)		=> writeByte(1); writeEngineAction_SetBeatRate(x)
		case x@EngineAction.ControlPlayer(_,_)	=> writeByte(2); writeEngineAction_ControlPlayer(x)
		}
	}
	def writeEngineAction_ChangeControl(it:EngineAction.ChangeControl):Unit = {
		writeDouble(it.speaker)
		writeDouble(it.phone)
	}
	def writeEngineAction_SetBeatRate(it:EngineAction.SetBeatRate):Unit = {
		writeDouble(it.beatRate)
	}
	def writeEngineAction_ControlPlayer(it:EngineAction.ControlPlayer):Unit = {
		writeInt(it.player)
		writePlayerAction(it.action)
	}
	def writePlayerAction(it:PlayerAction):Unit = {
		it match {
		case x@PlayerAction.ChangeControl(_,_,_,_,_,_,_)	=> writeByte(0);	writePlayerChangeControl(x)
		case x@PlayerAction.SetNeedSync(_)					=> writeByte(1);	writePlayerSetNeedSync(x)
		case x@PlayerAction.SetFile(_)						=> writeByte(2);	writePlayerSetFile(x)
		case x@PlayerAction.SetRhythm(_)					=> writeByte(3);	writePlayerSetRhythm(x)
		case x@PlayerAction.SetRunning(_)					=> writeByte(4);	writePlayerSetRunning(x)
		case x@PlayerAction.PitchAbsolute(_,_)				=> writeByte(5);	writePlayerPitchAbsolute(x)
		case x@PlayerAction.PhaseAbsolute(_)				=> writeByte(6);	writePlayerPhaseAbsolute(x)
		case x@PlayerAction.PhaseRelative(_)				=> writeByte(7);	writePlayerPhaseRelative(x)
		case x@PlayerAction.PositionAbsolute(_)				=> writeByte(8);	writePlayerPositionAbsolute(x)
		case x@PlayerAction.PositionJump(_,_)				=> writeByte(9);	writePlayerPositionJump(x)
		case x@PlayerAction.PositionSeek(_)					=> writeByte(10);	writePlayerPositionSeek(x)
		case x@PlayerAction.DragAbsolute(_)					=> writeByte(11);	writePlayerDragAbsolute(x)
		case PlayerAction.DragEnd							=> writeByte(12);	writePlayerDragEnd(PlayerAction.DragEnd)
		case x@PlayerAction.ScratchRelative(_)				=> writeByte(13);	writePlayerScratchRelative(x)
		case PlayerAction.ScratchEnd						=> writeByte(14);	writePlayerScratchEnd(PlayerAction.ScratchEnd)
		case x@PlayerAction.LoopEnable(_)					=> writeByte(15);	writePlayerLoopEnable(x)
		case PlayerAction.LoopDisable						=> writeByte(16);	writePlayerLoopDisable(PlayerAction.LoopDisable)
		}
	}
	def writePlayerChangeControl(it:PlayerAction.ChangeControl):Unit	= {
		writeDouble(it.trim);
		writeDouble(it.filter);
		writeDouble(it.low);
		writeDouble(it.middle);
		writeDouble(it.high);
		writeDouble(it.speaker);
		writeDouble(it.phone);
	}
	def writePlayerSetNeedSync(it:PlayerAction.SetNeedSync):Unit	= {
		writeBoolean(it.needSync)
	}
	def writePlayerSetFile(it:PlayerAction.SetFile):Unit	= {
		writeOption(it.file, writePath)
	}
	def writePlayerSetRhythm(it:PlayerAction.SetRhythm):Unit	= {
		writeOption(it.rhythm, writeRhythm)
	}
	def writePlayerSetRunning(it:PlayerAction.SetRunning):Unit	= {
		writeBoolean(it.running)
	}
	def writePlayerPitchAbsolute(it:PlayerAction.PitchAbsolute):Unit	= {
		writeDouble(it.pitch)
		writeBoolean(it.keepSync)
	}
	def writePlayerPhaseAbsolute(it:PlayerAction.PhaseAbsolute):Unit	= {
		writeRhythmValue(it.position)
	}
	def writePlayerPhaseRelative(it:PlayerAction.PhaseRelative):Unit	= {
		writeRhythmValue(it.offset)
	}
	def writePlayerPositionAbsolute(it:PlayerAction.PositionAbsolute):Unit	= {
		writeDouble(it.frame)
	}
	def writePlayerPositionJump(it:PlayerAction.PositionJump):Unit	= {
		writeDouble(it.frame)
		writeRhythmUnit(it.rhythmUnit)
	}
	def writePlayerPositionSeek(it:PlayerAction.PositionSeek):Unit	= {
		writeRhythmValue(it.offset)
	}
	def writePlayerDragAbsolute(it:PlayerAction.DragAbsolute):Unit	= {
		writeDouble(it.v)
	}
	def writePlayerDragEnd(it:PlayerAction.DragEnd.type):Unit	= {}
	def writePlayerScratchRelative(it:PlayerAction.ScratchRelative):Unit	= {
		writeDouble(it.frames)
	}
	def writePlayerScratchEnd(it:PlayerAction.ScratchEnd.type):Unit	= {}
	def writePlayerLoopEnable(it:PlayerAction.LoopEnable):Unit	= {
		writeLoopDef(it.preset)
	}
	def writePlayerLoopDisable(it:PlayerAction.LoopDisable.type):Unit	= {}

	def writePath(it:Path):Unit	= {
		writeString(it.toString)
	}
	def writeRhythm(it:Rhythm):Unit	= {
		writeDouble(it.anchor)
		writeDouble(it.measure)
		writeSchema(it.schema)
	}
	def writeSchema(it:Schema):Unit	= {
		writeInt(it.measuresPerPhrase)
		writeInt(it.beatsPerMeasure)
	}
	def writeRhythmValue(it:RhythmValue):Unit	= {
		writeDouble(it.steps)
		writeRhythmUnit(it.unit)
	}
	def writeRhythmUnit(it:RhythmUnit):Unit	= {
		it match {
			case RhythmUnit.Phrase	=> writeByte(0)
			case RhythmUnit.Measure	=> writeByte(1)
			case RhythmUnit.Beat	=> writeByte(2)
		}
	}

	//------------------------------------------------------------------------------

	def writeOption[T](value:Option[T], writeSub:T=>Unit):Unit	= {
		value match {
			case Some(x)	=>
				writeBoolean(true)
				writeSub(x)
			case None	=>
				writeBoolean(false)
		}
	}

	def writeString(it:String):Unit	= {
		writeByteArray(it.getBytes("UTF-8"))
	}

	//------------------------------------------------------------------------------

	def writeByte(it:Byte):Unit	= {
		st.write(it)
	}
	def writeShort(it:Short):Unit	= {
		writeByte((it >> 8).toByte)
		writeByte((it >> 0).toByte)
	}
	def writeInt(it:Int):Unit	= {
		writeByte((it >> 24).toByte)
		writeByte((it >> 16).toByte)
		writeByte((it >>  8).toByte)
		writeByte((it >>  0).toByte)
	}
	def writeLong(it:Long):Unit	= {
		writeByte((it >> 56).toByte)
		writeByte((it >> 48).toByte)
		writeByte((it >> 40).toByte)
		writeByte((it >> 32).toByte)
		writeByte((it >> 24).toByte)
		writeByte((it >> 16).toByte)
		writeByte((it >>  8).toByte)
		writeByte((it >>  0).toByte)
	}

	def writeFloat(it:Float):Unit	= {
		writeInt(java.lang.Float.floatToIntBits(it))
	}
	def writeDouble(it:Double):Unit	= {
		writeLong(java.lang.Double.doubleToLongBits(it))
	}

	def writeBoolean(it:Boolean):Unit	= {
		writeByte(if (it) 1 else 0)
	}
	def writeChar(it:Char):Unit	= {
		writeShort(it.toShort)
	}

	def writeByteArray(it:Array[Byte]):Unit	= {
		writeInt(it.length)
		st.write(it)
	}
}
