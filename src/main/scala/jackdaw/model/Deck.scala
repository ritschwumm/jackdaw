package jackdaw.model

import java.io.File

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.lang._
import scutil.log._

import scaudio.sample.Sample
import scaudio.math._

import screact._
import screact.extra._
	
import jackdaw.Config
import jackdaw.range.PitchMath
import jackdaw.data._
import jackdaw.media.Metadata
import jackdaw.curve.BandCurve
import jackdaw.key._
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
final class Deck(strip:Strip, tone:Tone, notifyPlayer:Effect[PlayerAction], playerFeedback:Signal[PlayerFeedback]) extends Observing with Logging {
	// NOTE these are set by the DeckUI
	val scratching	= cell[Option[Double]](None)
	val dragging	= cell[Option[(Boolean,Boolean)]](None)
	
	private val track	= cell[Option[Track]](None)
	val trackSignal		= track.signal
	
	//------------------------------------------------------------------------------
	//## internal emitters
	
	private val runningEmitter	= emitter[PlayerAction]
	private val otherEmitter	= emitter[PlayerAction]
	
	private def setRunning(running:Boolean) {
		runningEmitter emit PlayerSetRunning(running)
	}
	
	// switches needSync off
	private def setPitch(pitch:Double) {
		otherEmitter emit PlayerPitchAbsolute(pitch, false)
	}
	def setPitchOctave(octave:Double) {
		setPitch(octave2frequency(octave))
	}
	// keeps needSync as it is
	private def unPitch() {
		otherEmitter emit PlayerPitchAbsolute(unitFrequency, true)
	}
	
	def jumpFrame(frame:Double, fine:Boolean) {
		val rhythmUnit	= fine cata (Measure, Beat)
		otherEmitter emit PlayerPositionJump(frame, rhythmUnit)
	}
	
	def seek(steps:Int, fine:Boolean) {
		val offset	= RhythmValue(steps, fine cata (Measure, Beat))
		otherEmitter emit PlayerPositionSeek(offset)
	}
	
	def syncPhase(scale:RhythmUnit, fraction:Double) {
		val position	= RhythmValue(fraction, scale)
		otherEmitter emit PlayerPhaseAbsolute(position)
	}
	
	def movePhase(scale:RhythmUnit, steps:Double, fine:Boolean) {
		val offset	= RhythmValue(steps * (Deck phaseMoveOffset fine), scale)
		otherEmitter emit PlayerPhaseRelative(offset)
 	}
	
 	def modifyPhase(scale:RhythmUnit, fraction:Double) {
		val offset	= RhythmValue(fraction, scale)
		otherEmitter emit PlayerPhaseRelative(offset)
 	}
	
 	private def emitSetNeedSync(needSync:Boolean) {
 		otherEmitter	emit PlayerSetNeedSync(needSync)
 	}
	
 	private def emitLooping(size:Option[LoopDef]) {
 		otherEmitter emit (
 			size cata (PlayerLoopDisable, PlayerLoopEnable)
 		)
 	}
	
	//------------------------------------------------------------------------------
	//## track derivates

	private val trackWrap:OptionSignal[Track]	= OptionSignal(track.signal)
	val sample:Signal[Option[Sample]]			= (trackWrap flatMap { it => OptionSignal(it.sample)	}).unwrap
	val bandCurve:Signal[Option[BandCurve]]		= (trackWrap flatMap { it => OptionSignal(it.bandCurve)	}).unwrap
	val metadata:Signal[Option[Metadata]]		= (trackWrap flatMap { it => OptionSignal(it.metadata)	}).unwrap
	val rhythm:Signal[Option[Rhythm]]			= (trackWrap flatMap { it => OptionSignal(it.rhythm)	}).unwrap
	val wav:Signal[Option[File]]				= (trackWrap flatMap { it => OptionSignal(it.wav)		}).unwrap
	val	key:Signal[Option[MusicKey]]			= (trackWrap flatMap { it => OptionSignal(it.key)		}).unwrap
	val fileName:Signal[Option[String]]			= signal { track.current map	{ _.fileName				} }
	val cuePoints:Signal[Option[ISeq[Double]]]	= signal { track.current map	{ _.cuePoints.current		} }
	val annotation:Signal[Option[String]]		= signal { track.current map	{ _.annotation.current		} }
	val dataLoaded:Signal[Boolean]				= signal { track.current exists	{ _.dataLoaded.current		} }
	val sampleLoaded:Signal[Boolean]			= signal { track.current exists	{ _.sampleLoaded.current	} }
	// val fullyLoaded:Signal[Boolean]			= signal { track.current exists	{ _.fullyLoaded.current		} }
	
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
	
	private val pitch:Signal[Double]	= playerFeedback map { _.pitch	}
	
	/** pitch in octaves */
	val pitchOctave:Signal[Double]		= pitch map frequency2octave
	/** whether the player's pitch is non-unit */
	val pitched:Signal[Boolean]			= pitch map { _ != unitFrequency }
	
	// Some(None) means silence
	val effectiveKey:Signal[Option[Option[DetunedChord]]]	=
			signal {
				val baseKeyOpt		= key.current
				val semitoneOffset	= pitchOctave.current * 12
				baseKeyOpt map {
					_.toMusicChordOption map {
						_ detuned semitoneOffset
					}
				}
			}
			
	//------------------------------------------------------------------------------
	//## position
	
	val rhythmAnchor:Signal[Option[Double]] =
			signal {
				for {
					rhythm	<- rhythm.current
				}
				yield rhythm.anchor
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
				val rhy	= rhythm.current
				val pos	= position.current
				for {
					rhythm	<- rhy
				}
				yield rhythm index pos
			}
			
	/** how far it is from the player position to the next cue point */
	val playerBeforeCuePoint:Signal[Option[RhythmIndex]]	=
			signal {
				val rhy	= rhythm.current
				val cpf	= cuePointsFlat.current
				val pos	= position.current
				for {
					rhythm		<- rhy
					cuePoint	<- cpf find { _ > pos }
				}
				yield rhythm withAnchor cuePoint index pos
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
			
	private val dragMode	= state(dragSpeed,	PlayerDragAbsolute,		PlayerDragEnd)
	private val scratchMode	= state(scratching,	PlayerScratchRelative,	PlayerScratchEnd)
	
	private def state[S,T](input:Signal[Option[S]], move:S=>T, end:T):Events[T]	=
			(input.edge.filterOption map move)	orElse
			(input.edge map { it => !it.isEmpty } tag end)

	//------------------------------------------------------------------------------
	//## autocue

	private val newTrack:Events[Unit]	= {
		val switchedToLoadedTrack:Events[Unit]	= (track.edge.filterOption snapshotOnly dataLoaded).trueUnit
		val existingTrackGotLoaded:Events[Unit]	= track flatMapEvents { _ cata (never, _.dataLoaded.edge.trueUnit) }
		switchedToLoadedTrack orElse existingTrackGotLoaded
	}
	
	private val gotoCueOnLoad:Events[PlayerPositionAbsolute]	=
			newTrack snapshotOnly cuePointsFlat map { _ lift 0 getOrElse 0.0 into PlayerPositionAbsolute.apply }
	
	//------------------------------------------------------------------------------
	//## actions
	
	def loadTrack(file:File) {
		// TODO ugly hack
		val done	= track.current.exists { _.file ==== file }
		if (done) {
			INFO("rejected drop of already loaded file", file)
			return
		}
		
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
	
	private val changeControl:Signal[PlayerAction]	=
			signal {
				PlayerChangeControl(
					trim		= tone.trimGain.current,
					filter		= tone.filterValue.current,
					low			= tone.lowGain.current,
					middle		= tone.middleGain.current,
					high		= tone.highGain.current,
					speaker		= strip.speakerGain.current,
					phone		= strip.phoneGain.current
				)
			}
	changeControl observeNow notifyPlayer
	
	//sample	map PlayerSetSample.apply	observeNow	notifyPlayer
	wav		map PlayerSetFile.apply		observeNow	notifyPlayer
	rhythm	map PlayerSetRhythm.apply	observeNow	notifyPlayer
	
	private val playerActions:Events[PlayerAction]	=
			Events multiOrElse Vector(
				runningEmitter,
				otherEmitter,
				dragMode,
				scratchMode,
				gotoCueOnLoad
			)
	playerActions observe	notifyPlayer
	
	//------------------------------------------------------------------------------
	//## wiring
	
	private val autoPitchReset:Events[Unit]	=
			(track map { _.isDefined }).edge.falseUnit	orElse
			(newTrack snapshotOnly rhythm map { _.isDefined }).falseUnit
	
	private val autoToneReset:Events[Unit]	=
			track.edge tag (())
		
	autoPitchReset	observe { _ => unPitch() }
	autoToneReset	observe { _ => tone.resetAll()  }
}
