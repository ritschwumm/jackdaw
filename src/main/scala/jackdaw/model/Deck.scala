package jackdaw.model

import java.io.File

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import scaudio.sample.Sample
import scaudio.math._

import screact._
import screact.extra._
	
import jackdaw.Config
import jackdaw.audio._
import jackdaw.player._

import jackdaw.player.PlayerAction

object Deck {
	import Config.{ curveRaster => cr }
	import PitchMath.cents
	
	private val pitchFactor:Boolean=>Double				= _ cata (cents(5),		cents(2))
	private val stopDragSpeed:Boolean=>Double			= _ cata (1.0/4,		1.0/8) 
	private val playDragSpeed:Boolean=>Double			= _ cata (cents(200),	cents(100))
	private val independentRhythmFactor:Boolean=>Double	= _ cata (2.0,			1.5)
	private val cursorRhythmFactor:Boolean=>Double		= _ cata (cr,			cr/2)
	private val phaseMoveOffset:Boolean=>Double			= _ cata (1.0/16,		1.0/384)
}

/** model for a single deck */
final class Deck(strip:Strip, tone:Tone, notifyPlayer:Effect[ISeq[PlayerAction]], playerFeedback:Signal[PlayerFeedback]) extends Observing with Logging {
	// NOTE these are set by the DeckUI
	val track		= cell[Option[Track]](None)
	val scratching	= cell[Option[Double]](None)
	val dragging	= cell[Option[(Boolean,Boolean)]](None)
	
	//------------------------------------------------------------------------------
	//## internal emitters
	
	private val runningEmitter	= emitter[PlayerAction]
	private val otherEmitter	= emitter[PlayerAction]
	
	private def setRunning(running:Boolean) {
		runningEmitter emit (running cata (PlayerAction.RunningOff, PlayerAction.RunningOn))
	}
	
	def setPitch(pitch:Double) {
		otherEmitter emit PlayerAction.PitchAbsolute(pitch)
	}
	def setPitchOctave(octave:Double) {
		setPitch(octave2frequency(octave))
	}
	
	def jumpFrame(frame:Double, fine:Boolean) {
		val rhythmUnit	= fine cata (Measure, Beat)
		otherEmitter emit PlayerAction.PositionJump(frame, rhythmUnit)
	}
	
	def seek(steps:Int, fine:Boolean) {
		val offset	= RhythmValue(steps, fine cata (Measure, Beat))
		otherEmitter emit PlayerAction.PositionSeek(offset)
	}
	
	def syncPhaseManually(position:RhythmValue) {
		otherEmitter emit PlayerAction.PhaseAbsolute(position)
	}
	
	def movePhase(offset:RhythmValue, fine:Boolean) {
		val scaled	= offset scale (Deck phaseMoveOffset fine)
		otherEmitter emit PlayerAction.PhaseRelative(scaled)
 	}
 	
 	private def emitSetNeedSync(needSync:Boolean) {
 		otherEmitter	emit PlayerAction.SetNeedSync(needSync)
 	}
 	
 	private def emitLooping(size:Option[LoopDef]) {
 		otherEmitter emit (
 			size cata (PlayerAction.LoopDisable, PlayerAction.LoopEnable)
 		)
 	}
 	
	//------------------------------------------------------------------------------
	//## track derivates

	private val trackWrap:OptionSignal[Track]	= OptionSignal(track.signal)
	val sample:Signal[Option[Sample]]			= (trackWrap flatMap { it => OptionSignal(it.sample)	}).unwrap
	val bandCurve:Signal[Option[BandCurve]]		= (trackWrap flatMap { it => OptionSignal(it.bandCurve)	}).unwrap
	val metadata:Signal[Option[Metadata]]		= (trackWrap flatMap { it => OptionSignal(it.metadata)	}).unwrap
	val rhythm:Signal[Option[Rhythm]]			= (trackWrap flatMap { it => OptionSignal(it.rhythm)	}).unwrap
	val fileName:Signal[Option[String]]			= signal { track.current map	{ _.fileName			} }
	val cuePoints:Signal[Option[ISeq[Double]]]	= signal { track.current map	{ _.cuePoints.current	} }
	val annotation:Signal[Option[String]]		= signal { track.current map	{ _.annotation.current	} }
	val dataLoaded:Signal[Boolean]				= signal { track.current exists	{ _.dataLoaded.current	} }
	val fullyLoaded:Signal[Boolean]				= signal { track.current exists	{ _.fullyLoaded.current	} }
	
	val cuePointsFlat:Signal[ISeq[Double]]		= signal { cuePoints.current.flattenMany }
	
	//------------------------------------------------------------------------------
	//## player derivates
	
	val running			= playerFeedback map { _.running		} 
	val afterEnd		= playerFeedback map { _.afterEnd		} 
	val position		= playerFeedback map { _.position		} 
	val measureMatch	= playerFeedback map { _.measureMatch	}
	val beatRate		= playerFeedback map { _.beatRate		}
	val loopSpan		= playerFeedback map { _.loopSpan		}
	val loopDef			= playerFeedback map { _.loopDef		}
	
	val synced:Signal[Option[Boolean]]	=
			signal {
				val feedback	= playerFeedback.current
				(feedback.needSync, feedback.hasSync) match {
					case (true,  true)	=> Some(true)
					case (true,  false)	=> Some(false)
					case (false, true)	=> Some(false)
					case (false, false)	=> None
				}
			}
	
	private val pitch	= playerFeedback map { _.pitch	}
	
	/** pitch in octaves */
	val pitchOctave	= pitch map frequency2octave
	/** whether the player's pitch is non-unit */
	val pitched		= pitch map { _ != unitFrequency }
	
	//------------------------------------------------------------------------------
	//## position
	
	/** all rhythm lines to be displayed */
	val rhythmLines:Signal[Option[ISeq[RhythmLine]]] =
			signal {
				for { 
					rhythm	<- rhythm.current
					sample	<- sample.current
				}
				yield rhythm lines (0, sample.frameCount)
			}
	
	/*
	// number of seconds until the track ends
	val playerRemainingSeconds:Signal[Option[Double]]	=
			signal {
				for {
					sample	<- sample.current
				}
				yield (sample.frameCount.toDouble - position.current) / sample.frameRate
			}
	*/
	
	/** player position (almost) floored to rhythm points */
	val playerRhythmIndex:Signal[Option[RhythmIndex]]	=
			signal {
				for {
					rhythm	<- rhythm.current
				}
				yield rhythm index position.current
			}
			
	/** how far it is from the player position to the next cue point */
	val playerBeforeCuePoint:Signal[Option[Int]]	=
			signal {
				for {
					rhythm		<- rhythm.current
					measures	<- calculateNextCuepoint(rhythm, cuePointsFlat.current, position.current)
				}
				yield measures
			}
			
	private def calculateNextCuepoint(rhythm:Rhythm, cuePoints:ISeq[Double], position:Double):Option[Int]	=
			cuePoints map { _ - position } find { _ > 0 } map rhythm.measureDistance
	
	//------------------------------------------------------------------------------
	//## gui derivates
	
	/**
	drag behaviour depends on the player running or not.
	if it is running, we multiply the pitch to make it faster or slower
	if it is stopped, we add to the speed to go forward or backwards
	*/
	private val dragSpeed:Signal[Option[Double]]	=
			signal {
				dragging.current map { case (forward, fine) =>
					running.current cata (
						additive(
							forward, zeroFrequency, 
							Deck stopDragSpeed fine
						),
						multiplicative(
							forward, pitch.current, 
							Deck playDragSpeed fine
						)
					)
				}
			}
	
	// make start/move/end from continuous option
			
	private val draggingState		= state(dragSpeed,	PlayerAction.DragBegin,		PlayerAction.DragEnd)
	private val scratchingState		= state(scratching,	PlayerAction.ScratchBegin,	PlayerAction.ScratchEnd)
	
	private def state[T](input:Signal[Option[_]], begin:T, end:T):Events[T]	=
			(input map { _.isDefined cata (end, begin) }).edge
	
	private val draggingAbsolute	= move(dragSpeed,	PlayerAction.DragAbsolute.apply)
	private val scratchingRelative	= move(scratching,	PlayerAction.ScratchRelative.apply)

	private def move[S,T](input:Signal[Option[S]], move:S=>T):Events[T]	=
			input.edge.filterOption map move

	//------------------------------------------------------------------------------
	//## autocue

	private val gotoCueOnLoad:Events[PlayerAction.PositionAbsolute]	= {
		val switchedToLoadedTrack:Events[Unit]	= (track.edge.filterOption snapshotOnly dataLoaded).trueUnit
		val existingTrackGotLoaded:Events[Unit]	= track flatMapEvents { _ cata (never, _.dataLoaded.edge.trueUnit) }
		val jumpToCueNow:Events[Unit]			= switchedToLoadedTrack orElse existingTrackGotLoaded
		jumpToCueNow snapshotOnly cuePointsFlat map { _ lift 0 getOrElse 0.0 into PlayerAction.PositionAbsolute.apply }
	}
	
	//------------------------------------------------------------------------------
	//## actions
	
	def loadTrack(file:File) {
		setRunning(false)
		track set (Track load file)
	}
	
	def ejectTrack() {
		setRunning(false)
		track set None
		tone.resetAll()
	}
	
	def syncToggle() {
		emitSetNeedSync(!playerFeedback.current.needSync)
	}
	
	def playToggle() {
		setRunning(!running.current)
	}
	
	def setLoop(preset:Option[LoopDef]) {
		emitLooping(preset)
	}
	
	def jumpCue(index:Int, fine:Boolean) {
		cuePointsFlat.current lift index foreach { frame =>
			changeTrack { track =>
				jumpFrame(frame, fine)
			}
		}
	}
	
	def addCue(fine:Boolean) {
		changeTrack {
			_ addCuePoint (
				position.current,
				fine cata (Measure, Beat)
			)
		}
	}
	
	def removeCue() {
		changeTrack {
			_ removeCuePoint position.current
		}
	}
	
	def resetPitch() {
		setPitch(unitFrequency)
	}
	
	// TODO ugly
	def changePitch(steps:Int, fine:Boolean) {
		setPitch(
			pitch.current * 
			octave2frequency(
				frequency2octave(Deck pitchFactor fine) * 
				steps
			)
		)
	}
	
	// private def pitchStep	=
	// 		Deck pitchFactor decouple(fineModifier.current)
	
	def setAnnotation(it:String) {
		changeTrack { 
			_ setAnnotation it
		}
	}
	
	def changeRhythmAnchor() {
		changeTrack { 
			_ setRhythmAnchor position.current
		}
	}
	
	def toggleRhythm() {
		changeTrack { 
			_ toogleRhythm position.current
		}
	}
	
	def moveRhythmBy(positive:Boolean, fine:Boolean) {
		changeTrack { 
			_ moveRhythmBy additive(
				positive, 0,
				Deck cursorRhythmFactor fine
			)
		}
	}
	
	def resizeRhythmBy(positive:Boolean, fine:Boolean) {
		changeTrack { 
			_ resizeRhythmBy multiplicative(
				positive, 1, 
				Deck independentRhythmFactor fine
			)
		}
	}
	
	def resizeRhythmAt(positive:Boolean, fine:Boolean) {
		changeTrack { 
			_ resizeRhythmAt (
				position.current, 
				additive(
					positive, 0, 
					Deck cursorRhythmFactor fine
				)
			) 
		}
	}
	
	private def changeTrack(effect:Effect[Track]) {
		track.current foreach effect
	}
	
	//------------------------------------------------------------------------------
	//## helper
	
	private def multiplicative(positive:Boolean, base:Double, change:Double):Double =
			positive cata (base / change, base * change)	

	private def additive(positive:Boolean, base:Double, change:Double):Double =
			positive cata (base - change, base + change)	
			
	//------------------------------------------------------------------------------
	//## player control
	
	private val changeControl	=
			signal {
				Vector(PlayerAction.ChangeControl(
					trim		= tone.trimGain.current,
					filter		= tone.filterValue.current,
					low			= tone.lowGain.current,
					middle		= tone.middleGain.current,
					high		= tone.highGain.current,
					speaker		= strip.speakerGain.current,
					phone		= strip.phoneGain.current,
					sample		= sample.current,
					rhythm		= rhythm.current
				))
			}
	changeControl observeNow notifyPlayer

	private val playerActions:Events[ISeq[PlayerAction]]	=
			events {
				Vector(
					runningEmitter.message,
					otherEmitter.message,
					scratchingState.message,
					scratchingRelative.message,
					draggingState.message,
					draggingAbsolute.message,
					gotoCueOnLoad.message
				).flatten guardBy { _.nonEmpty }
			}
	playerActions observe	notifyPlayer
	
	//------------------------------------------------------------------------------
	//## wiring
	
	private val autoPitchReset:Events[Unit]	=
			(track.edge snapshotOnly synced map { _.isEmpty }).trueUnit
		
	private val autoToneReset:Events[Unit]	=
			track.edge tag (())
		
	autoPitchReset	observe { _ => setPitch(unitFrequency) }
	autoToneReset	observe { _ => tone.resetAll()  }
}
