package jackdaw.remote

import java.io._

import scutil.lang.implicits._
import jackdaw.player._
import jackdaw.data._
	
final class Input(val st:InputStream) {
	def readToStub():ToStub	=
			readByte() match {
				case 0	=> readStartedStub()
				case 1	=> readSendStub()
				case x	=> sys error show"unexpected tag $x"
			}
	def readStartedStub():StartedStub	=
			StartedStub(
				outputRate		= readInt(),
				phoneEnabled	= readBoolean()
			)
	def readSendStub():SendStub	=
			SendStub(
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
				measureMatch	= readOption(readDouble),
				beatRate		= readOption(readDouble),
				needSync		= readBoolean(),
				hasSync			= readBoolean(),
				masterPeak		= readFloat(),
				loopSpan		= readOption(readSpan),
				loopDef			= readOption(readLoopDef)
			)
	def readSpan():Span	=
			Span(
				start	= readDouble(),
				size	= readDouble()
			)
	def readLoopDef():LoopDef	=
			LoopDef(
				measures	= readInt()
			)
			
	//------------------------------------------------------------------------------
	
	def readToSkeleton():ToSkeleton	=
			readByte() match {
				case 0	=> readKillSkeleton()
				case 1	=> readSendSkeleton()
				case x	=> sys error show"unexpected tag $x"
			}
	def readKillSkeleton():KillSkeleton.type	=
			KillSkeleton
	def readSendSkeleton():SendSkeleton	=
			SendSkeleton(
				action	= readEngineAction()
			)
	def readEngineAction():EngineAction	=
			readByte() match {
				case 0	=> readEngineChangeControl()
				case 1	=> readEngineSetBeatRate()
				case 2	=> readEngineControlPlayer()
				case x	=> sys error show"unexpected tag $x"
			}
	def readEngineChangeControl():EngineChangeControl	=
			EngineChangeControl(
				speaker	= readDouble(),
				phone	= readDouble()
			)
	def readEngineSetBeatRate():EngineSetBeatRate	=
			EngineSetBeatRate(
				beatRate	= readDouble()
			)
	def readEngineControlPlayer():EngineControlPlayer	=
			EngineControlPlayer(
				player	= readInt(),
				action	= readPlayerAction()
			)
	def readPlayerAction():PlayerAction	=
			readByte() match {
				case 0	=> readPlayerChangeControl()
				case 1	=> readPlayerSetNeedSync()
				case 2	=> readPlayerSetFile()
				case 3	=> readPlayerSetRhythm()
				case 4	=> readPlayerSetRunning()
				case 5	=> readPlayerPitchAbsolute()
				case 6	=> readPlayerPhaseAbsolute()
				case 7	=> readPlayerPhaseRelative()
				case 8	=> readPlayerPositionAbsolute()
				case 9	=> readPlayerPositionJump()
				case 10	=> readPlayerPositionSeek()
				case 11	=> readPlayerDragAbsolute()
				case 12	=> readPlayerDragEnd()
				case 13	=> readPlayerScratchRelative()
				case 14	=> readPlayerScratchEnd()
				case 15	=> readPlayerLoopEnable()
				case 16	=> readPlayerLoopDisable()
				case x	=> sys error show"unexpected tag $x"
			}
	def readPlayerChangeControl():PlayerChangeControl	=
			PlayerChangeControl(
				trim	= readDouble(),
				filter	= readDouble(),
				low		= readDouble(),
				middle	= readDouble(),
				high	= readDouble(),
				speaker	= readDouble(),
				phone	= readDouble()
			)
	def readPlayerSetNeedSync():PlayerSetNeedSync	=
			PlayerSetNeedSync(
				needSync	= readBoolean()
			)
	def readPlayerSetFile():PlayerSetFile	=
			PlayerSetFile(
				file	= readOption(readFile)
			)
	def readPlayerSetRhythm():PlayerSetRhythm	=
			PlayerSetRhythm(
				rhythm	= readOption(readRhythm)
			)
	def readPlayerSetRunning():PlayerSetRunning	=
			PlayerSetRunning(
				running	= readBoolean()
			)
	def readPlayerPitchAbsolute():PlayerPitchAbsolute	=
			PlayerPitchAbsolute(
				pitch		= readDouble(),
				keepSync	= readBoolean()
			)
	def readPlayerPhaseAbsolute():PlayerPhaseAbsolute	=
			PlayerPhaseAbsolute(
				position	= readRhythmValue()
			)
	def readPlayerPhaseRelative():PlayerPhaseRelative	=
			PlayerPhaseRelative(
				offset		= readRhythmValue()
			)
	def readPlayerPositionAbsolute():PlayerPositionAbsolute	=
			PlayerPositionAbsolute(
				frame	= readDouble()
			)
	def readPlayerPositionJump():PlayerPositionJump	=
			PlayerPositionJump(
				frame		= readDouble(),
				rhythmUnit	= readRhythmUnit()
			)
	def readPlayerPositionSeek():PlayerPositionSeek	=
			PlayerPositionSeek(
				offset		= readRhythmValue()
			)
	def readPlayerDragAbsolute():PlayerDragAbsolute	=
			PlayerDragAbsolute(
				v	= readDouble()
			)
	def readPlayerDragEnd():PlayerDragEnd.type	=
			PlayerDragEnd
	def readPlayerScratchRelative():PlayerScratchRelative	=
			PlayerScratchRelative(
				frames	= readDouble()
			)
	def readPlayerScratchEnd():PlayerScratchEnd.type	=
			PlayerScratchEnd
	def readPlayerLoopEnable():PlayerLoopEnable	=
			PlayerLoopEnable(
				preset	= readLoopDef()
			)
	def readPlayerLoopDisable():PlayerLoopDisable.type	=
			PlayerLoopDisable
		
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
				case 0	=> Phrase
				case 1	=> Measure
				case 2	=> Beat
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
			val len	= st read (out, i, length-i)
			if (len == -1)	sys error "unexpected end of stream"
			i	= i + len
		}
		out
	}
}
