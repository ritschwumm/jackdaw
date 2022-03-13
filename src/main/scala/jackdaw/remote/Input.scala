package jackdaw.remote

import java.io.*

import scutil.lang.implicits.*
import jackdaw.player.*
import jackdaw.data.*

final class Input(val st:InputStream) {
	def readToStub():ToStub	=
		readByte() match {
			case 0	=> readStartedStub()
			case 1	=> readSendStub()
			case x	=> sys error show"unexpected tag $x"
		}
	def readStartedStub():ToStub	=
		ToStub.Started(
			outputRate		= readInt(),
			phoneEnabled	= readBoolean()
		)
	def readSendStub():ToStub	=
		ToStub.Send(
			feedback	= readEngineFeedback()
		)
	def readEngineFeedback():EngineFeedback	=
		EngineFeedback(
			masterPeak	= readFloat(),
			player1		= readPlayerFeedback(),
			player2		= readPlayerFeedback(),
			player3		= readPlayerFeedback()
		)
	def readPlayerFeedback():PlayerFeedback	=
		PlayerFeedback(
			running			= readBoolean(),
			afterEnd		= readBoolean(),
			position		= readDouble(),
			pitch			= readDouble(),
			measureMatch	= readOption(readDouble()),
			beatRate		= readOption(readDouble()),
			needSync		= readBoolean(),
			hasSync			= readBoolean(),
			masterPeak		= readFloat(),
			loopSpan		= readOption(readSpan()),
			loopDef			= readOption(readLoopDef())
		)
	def readSpan():Span	=
		Span(
			start	= readDouble(),
			size	= readDouble()
		)
	def readLoopDef():LoopDef	=
		LoopDef.fromInput(
			measures	= readInt()
		)

	//------------------------------------------------------------------------------

	def readToSkeleton():ToSkeleton	=
		readByte() match {
			case 0	=> readToSkeletonKill()
			case 1	=> readToSkeletonSend()
			case x	=> sys error show"unexpected tag $x"
		}
	def readToSkeletonKill():ToSkeleton	=
		ToSkeleton.Kill
	def readToSkeletonSend():ToSkeleton	=
		ToSkeleton.Send(
			action	= readEngineAction()
		)
	def readEngineAction():EngineAction	=
		readByte() match {
			case 0	=> readEngineChangeControl()
			case 1	=> readEngineSetBeatRate()
			case 2	=> readEngineControlPlayer()
			case x	=> sys error show"unexpected tag $x"
		}
	def readEngineChangeControl():EngineAction	=
		EngineAction.ChangeControl(
			speaker	= readDouble(),
			phone	= readDouble()
		)
	def readEngineSetBeatRate():EngineAction	=
		EngineAction.SetBeatRate(
			beatRate	= readDouble()
		)
	def readEngineControlPlayer():EngineAction	=
		EngineAction.ControlPlayer(
			player	= readInt(),
			action	= readPlayerAction()
		)
	def readPlayerAction():PlayerAction	=
		readByte() match {
			case 0	=> readPlayerAction_PlayerChangeControl()
			case 1	=> readPlayerAction_PlayerSetNeedSync()
			case 2	=> readPlayerAction_PlayerSetFile()
			case 3	=> readPlayerAction_PlayerSetRhythm()
			case 4	=> readPlayerAction_PlayerSetRunning()
			case 5	=> readPlayerAction_PlayerPitchAbsolute()
			case 6	=> readPlayerAction_PlayerPhaseAbsolute()
			case 7	=> readPlayerAction_PlayerPhaseRelative()
			case 8	=> readPlayerAction_PlayerPositionAbsolute()
			case 9	=> readPlayerAction_PlayerPositionJump()
			case 10	=> readPlayerAction_PlayerPositionSeek()
			case 11	=> readPlayerAction_PlayerDragAbsolute()
			case 12	=> readPlayerAction_PlayerDragEnd()
			case 13	=> readPlayerAction_PlayerScratchRelative()
			case 14	=> readPlayerAction_PlayerScratchEnd()
			case 15	=> readPlayerAction_PlayerLoopEnable()
			case 16	=> readPlayerAction_PlayerLoopDisable()
			case x	=> sys error show"unexpected tag $x"
		}
	def readPlayerAction_PlayerChangeControl():PlayerAction	=
		PlayerAction.PlayerChangeControl(
			trim	= readDouble(),
			filter	= readDouble(),
			low		= readDouble(),
			middle	= readDouble(),
			high	= readDouble(),
			speaker	= readDouble(),
			phone	= readDouble()
		)
	def readPlayerAction_PlayerSetNeedSync():PlayerAction	=
		PlayerAction.PlayerSetNeedSync(
			needSync	= readBoolean()
		)
	def readPlayerAction_PlayerSetFile():PlayerAction	=
		PlayerAction.PlayerSetFile(
			file	= readOption(readFile())
		)
	def readPlayerAction_PlayerSetRhythm():PlayerAction	=
		PlayerAction.PlayerSetRhythm(
			rhythm	= readOption(readRhythm())
		)
	def readPlayerAction_PlayerSetRunning():PlayerAction	=
		PlayerAction.PlayerSetRunning(
			running	= readBoolean()
		)
	def readPlayerAction_PlayerPitchAbsolute():PlayerAction	=
		PlayerAction.PlayerPitchAbsolute(
			pitch		= readDouble(),
			keepSync	= readBoolean()
		)
	def readPlayerAction_PlayerPhaseAbsolute():PlayerAction	=
		PlayerAction.PlayerPhaseAbsolute(
			position	= readRhythmValue()
		)
	def readPlayerAction_PlayerPhaseRelative():PlayerAction	=
		PlayerAction.PlayerPhaseRelative(
			offset		= readRhythmValue()
		)
	def readPlayerAction_PlayerPositionAbsolute():PlayerAction	=
		PlayerAction.PlayerPositionAbsolute(
			frame	= readDouble()
		)
	def readPlayerAction_PlayerPositionJump():PlayerAction	=
		PlayerAction.PlayerPositionJump(
			frame		= readDouble(),
			rhythmUnit	= readRhythmUnit()
		)
	def readPlayerAction_PlayerPositionSeek():PlayerAction	=
		PlayerAction.PlayerPositionSeek(
			offset		= readRhythmValue()
		)
	def readPlayerAction_PlayerDragAbsolute():PlayerAction	=
		PlayerAction.PlayerDragAbsolute(
			v	= readDouble()
		)
	def readPlayerAction_PlayerDragEnd():PlayerAction	=
		PlayerAction.PlayerDragEnd
	def readPlayerAction_PlayerScratchRelative():PlayerAction	=
		PlayerAction.PlayerScratchRelative(
			frames	= readDouble()
		)
	def readPlayerAction_PlayerScratchEnd():PlayerAction	=
		PlayerAction.PlayerScratchEnd
	def readPlayerAction_PlayerLoopEnable():PlayerAction	=
		PlayerAction.PlayerLoopEnable(
			preset	= readLoopDef()
		)
	def readPlayerAction_PlayerLoopDisable():PlayerAction	=
		PlayerAction.PlayerLoopDisable

	def readFile():File	=
		new File(readString())
	def readRhythm():Rhythm	=
		Rhythm(
			anchor	= readDouble(),
			measure	= readDouble(),
			schema	= readSchema()
		)
	def readSchema():Schema	=
		Schema(
			measuresPerPhrase	= readInt(),
			beatsPerMeasure		= readInt()
		)
	def readRhythmValue():RhythmValue	=
		RhythmValue(
			steps	= readDouble(),
			unit	= readRhythmUnit()
		)
	def readRhythmUnit():RhythmUnit	=
		readByte() match {
			case 0	=> RhythmUnit.Phrase
			case 1	=> RhythmUnit.Measure
			case 2	=> RhythmUnit.Beat
			case x	=> sys error show"unexpected tag $x"
		}

	//------------------------------------------------------------------------------

	def readOption[T](delegate: =>T):Option[T]	= {
		if (readBoolean())	Some(delegate)
		else				None
	}

	def readString():String	= {
		new String(readByteArray(), "UTF-8")
	}

	//------------------------------------------------------------------------------

	def readByte():Byte	= {
		val x	= st.read()
		if (x == -1) sys error "unexpected end of stream"
		x.toByte
	}
	def readShort():Short = {
		(	((readByte() & 0xff) << 8)	|
			((readByte() & 0xff) << 0)
		).toShort
	}
	def readInt():Int = {
		((readByte() & 0xff) << 24)	|
		((readByte() & 0xff) << 16)	|
		((readByte() & 0xff) <<  8)	|
		((readByte() & 0xff) <<  0)
	}
	def readLong():Long = {
		((readByte() & 0xff).toLong << 56)	|
		((readByte() & 0xff).toLong << 48)	|
		((readByte() & 0xff).toLong << 40)	|
		((readByte() & 0xff).toLong << 32)	|
		((readByte() & 0xff).toLong << 24)	|
		((readByte() & 0xff).toLong << 16)	|
		((readByte() & 0xff).toLong <<  8)	|
		((readByte() & 0xff).toLong <<  0)
	}

	def readFloat():Float	= {
		java.lang.Float.intBitsToFloat(readInt())
	}
	def readDouble():Double	= {
		java.lang.Double.longBitsToDouble(readLong())
	}

	def readBoolean():Boolean	= {
		readByte() != 0
	}
	def readChar():Char	= {
		readShort().toChar
	}

	def readByteArray():Array[Byte]	= {
		val length	= readInt()
		val out	= new Array[Byte](length)
		var i = 0
		while (i < length) {
			val len	= st.read(out, i, length-i)
			if (len == -1)	sys error "unexpected end of stream"
			i	= i + len
		}
		out
	}
}
