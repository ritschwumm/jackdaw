package jackdaw.model

import java.io.File

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.log.Logging

import scaudio.sample.Sample
import scaudio.math._

import screact._
import screact.extra._
	
import jackdaw.Config
import jackdaw.audio._
import jackdaw.model._
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
	
	private val runningEmitter	= emitter[PlayerAction.Running]
	private val pitchEmitter	= emitter[PlayerAction.PitchAbsolute]
	private val gotoEmitter		= emitter[PlayerAction.PositionAbsolute]
	private val jumpEmitter		= emitter[PlayerAction.PositionJump]
	private val seekEmitter		= emitter[PlayerAction.PositionSeek]
	private val phaseEmitter	= emitter[PlayerAction.Phase]
	private val needSyncEmitter	= emitter[PlayerAction.SetNeedSync]
	
	private def setRunning(running:Boolean) {
		runningEmitter emit (running cata (PlayerAction.RunningOff, PlayerAction.RunningOn))
	}
	
	def setPitch(pitch:Double) {
		pitchEmitter emit PlayerAction.PitchAbsolute(pitch)
	}
	def setPitchOctave(octave:Double) {
		setPitch(octave2frequency(octave))
	}
	
	private def gotoFrame(frame:Double) {
		gotoEmitter emit PlayerAction.PositionAbsolute(frame)
	}
	
	def jumpFrame(frame:Double, fine:Boolean) {
		val rhythmUnit	= fine cata (RhythmUnit.Measure, RhythmUnit.Beat)
		jumpEmitter emit PlayerAction.PositionJump(frame, rhythmUnit)
	}
	
	def seek(steps:Int, fine:Boolean) {
		seekEmitter emit PlayerAction.PositionSeek(steps, fine cata (RhythmUnit.Measure, RhythmUnit.Beat))
	}
	
	def syncPhaseManually(rhythmUnit:RhythmUnit, offset:Double) {
		phaseEmitter emit PlayerAction.PhaseAbsolute(rhythmUnit, offset)
	}
	
	def movePhase(rhythmUnit:RhythmUnit, steps:Double, fine:Boolean) {
		val offset	= (Deck phaseMoveOffset fine) * steps
		phaseEmitter emit PlayerAction.PhaseRelative(rhythmUnit, offset)
 	}
 	
 	private def emitSetNeedSync(needSync:Boolean) {
 		needSyncEmitter	emit PlayerAction.SetNeedSync(needSync)
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
	val loaded:Signal[Boolean]					= signal { track.current exists	{ _.loaded.current		} }
	
	val cuePointsFlat	= signal { cuePoints.current.flattenMany }
	
	//------------------------------------------------------------------------------
	//## player derivates
	
	val running			= playerFeedback map { _.running		} 
	val afterEnd		= playerFeedback map { _.afterEnd		} 
	val position		= playerFeedback map { _.position		} 
	val measureMatch	= playerFeedback map { _.measureMatch	}
	val beatRate		= playerFeedback map { _.beatRate		}
	
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
	
	/*
	// number of seconds until the track ends
	val remainingSeconds:Signal[Option[Double]]	=
			signal {
				for {
					sample	<- sample.current
				}
				yield (sample.frameCount.toDouble - position.current) / sample.frameRate
			}
	*/
	
	/** how far it is from the player position to the next cue point */
	val beforeCuePoint:Signal[Option[Int]]	=
			signal {
				// NOTE cue points are always sorted by position
				val positionCur	= position.current
				val nextOpt		= (cuePointsFlat.current dropWhile { _ <= positionCur }).headOption
				for {
					rhythm	<- rhythm.current
					next	<- nextOpt
				}
				yield rhythm measureDistance (next - positionCur)
			}
	
	/** player position (almost) floored to rhythm points */
	val rhythmIndex:Signal[Option[RhythmIndex]]	=
			signal {
				for {
					rhythm	<- rhythm.current
				}
				yield rhythm index position.current
			}
	
	/** all rhythm lines to be displayed */
	val rhythmLines:Signal[Option[ISeq[RhythmLine]]] =
			signal {
				for { 
					rhythm	<- rhythm.current
					sample	<- sample.current
				}
				yield rhythm lines (0, sample.frameCount)
			}
	
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
									Deck stopDragSpeed fine),
							multiplicative(
									forward, pitch.current, 
									Deck playDragSpeed fine))
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
	//## autosync

	// BETTER less ugly
	private val trackGotLoaded:Events[Unit]	= {
		// track goes to Some while loaded is true
		val switchedToLoadedTrack:Events[Unit]	= (track.edge.filterOption snapshotOnly loaded).trueUnit
		// loaded goes to true while track is Some
		val currentTrackGotLoaded:Events[Unit]	= track flattenEvents { _ cata (never, _.loaded.edge.trueUnit) }
		// loaded
		switchedToLoadedTrack orElse currentTrackGotLoaded
	}
		
	// TODO should be done as soon as the cue has been provided! 
	private val gotoCueOnLoad:Events[PlayerAction.PositionAbsolute]	= {
		val loadPoint:Signal[Double]	= cuePointsFlat map { _ lift 0 getOrElse 0.0 }
		trackGotLoaded snapshotOnly loadPoint map PlayerAction.PositionAbsolute.apply
	}
	
	//------------------------------------------------------------------------------
	//## actions
	
	def loadTrack(file:File) {
		setRunning(false)
		track set (Track load file )
	}
	
	def ejectTrack() {
		setRunning(false)
		track set None
	}
	
	def syncToggle() {
		emitSetNeedSync(!playerFeedback.current.needSync)
	}
	
	def playToggle() {
		setRunning(!running.current)
	}
	
	def jumpCue(index:Int, fine:Boolean) {
		cuePointsFlat.current lift index foreach { frame =>
			changeTrack { track =>
				// setRunning(false)
				// gotoFrame(frame)
				jumpFrame(frame, fine)
			}
		}
	}
	
	def addCue(fine:Boolean) {
		changeTrack {
			_ addCuePoint (
					position.current,
					fine cata (RhythmUnit.Measure, RhythmUnit.Beat))
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
						steps))
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
			_	setRhythmAnchor	position.current
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
					Deck cursorRhythmFactor fine)
		}
	}
	
	def resizeRhythmBy(positive:Boolean, fine:Boolean) {
		changeTrack { 
			_ resizeRhythmBy multiplicative(
					positive, 1, 
					Deck independentRhythmFactor fine)
		}
	}
	
	def resizeRhythmAt(positive:Boolean, fine:Boolean) {
		changeTrack { 
			_ resizeRhythmAt (
					position.current, 
					additive(
							positive, 0, 
							Deck cursorRhythmFactor fine)) 
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
					pitchEmitter.message,		
					phaseEmitter.message,
					gotoEmitter.message,	  
					jumpEmitter.message, 
					seekEmitter.message,
					scratchingState.message,
					scratchingRelative.message,
					draggingState.message,		
					draggingAbsolute.message,
					// syncPhaseEvents.message,
					// syncSpeedEvents.message,
					gotoCueOnLoad.message,
					needSyncEmitter.message
				).flatten guardBy { _.nonEmpty }
			}
	playerActions observe	notifyPlayer
}
