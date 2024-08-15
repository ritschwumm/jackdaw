package jackdaw.player

import scala.annotation.nowarn

import java.nio.file.Path

import scala.math.*

import scutil.core.implicits.*
import scutil.lang.*
import scutil.math.functions.*
import scutil.bit.*

import scaudio.control.*
import scaudio.output.*
import scaudio.sample.*
import scaudio.interpolation.*
import scaudio.math.*
import scaudio.dsp.*

import jackdaw.*
import jackdaw.data.*
import jackdaw.concurrent.Target

object Player {
	private val dampTime	= 0.1	// 0..1 range in 1/10 of a second
	private val fadeTime	= 0.02	// 20 ms
	private val endDelay	= 0.1	// 100 ms
	private val noEnd		= -1d

	val springPitchLimit	= 16
	private val springDamping	= 0.8	// a factor
	private val springHardness	= 2000.0
	private val springMass		= 2.0

	// filter modes
	private val filterLP	= -1
	private val filterOff	= 0
	private val filterHP	= +1

	// ignore small aberrations
	private val	positionEpsilon	= 1.0E-4
	private val filterEpsilon	= 1.0E-5

	private val interpolation	= Config.sincEnabled.cata(Linear, Sinc)

	// in frames
	val maxDistance	= interpolation.overshot(springPitchLimit)

	// headFrame, fadeFrame, jump, loop
	val headCount		= 4

	private val passCoeffs	=  BiQuadCoeffs(1,0,0,0,0)
}

/**
 * one audio line outputting audio data for one Deck using a MixHalf
 * public methods must never be called outside the engine thread
 */
final class Player(metronome:Metronome, outputRate:Double, phoneEnabled:Boolean, loaderTarget:Target[LoaderAction]) {
	private val equalizerL	= new Equalizer(Config.lowEq, Config.highEq, outputRate)
	private val equalizerR	= new Equalizer(Config.lowEq, Config.highEq, outputRate)

	private val filterL		= new BiQuad
	private val filterR		= new BiQuad

	private var sample:Option[CacheSample]	= None
	private var rhythm:Option[Rhythm]		= None
	private var inputL:Channel				= Channel.empty
	private var inputR:Channel				= Channel.empty

	// 0..1 range
	private val trim		= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)
	// -1..+1
	private val filter		= DamperDouble.forRates(0.0,		Player.dampTime, outputRate)
	// 0..1 range
	private val low			= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)
	private val middle		= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)
	private val high		= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)
	// 0..1 range
	private val speaker		= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)
	private val phone		= DamperDouble.forRates(unitGain,	Player.dampTime, outputRate)

	private val peakDetector	= new PeakDetector

	private var mode:PlayerMode	= PlayerMode.Playing

	private var running		= false
	private var needSync	= true

	private val deltaTime	= 1.0 / outputRate	// time between two frames

	private var pitch		= unitFrequency
	// of the sample itself
	private var rate		= zeroFrequency

	private var velocity		= 0.0
	private var	acceleration	= 0.0	// non-zero when scratching

	private val springSpeedLimit	= Player.springPitchLimit * outputRate
	// TODO do i need both of those?
	private var scratchBaseFrame	= 0.0
	private var springOriginFrame	= 0.0

	private var	fadeFrame	= 0.0	// position fading out
	private var	headFrame	= 0.0	// position fading in

	private var endFrame	= Player.noEnd
	@inline
	private def afterEnd	= endFrame != Player.noEnd && headFrame > endFrame

	private val fadeStep		= 1.0 / (Player.fadeTime * outputRate)
	private val fadeMin			= 0.0
	private val fadeMax			= 1.0
	private var fadeValue		= fadeMax
	private var fadeProgress	= false
	private var fadeLater:Option[Fade]	= None

	private var jumpLater:Option[Thunk[Unit]]	= None
	private var jumpProgress:Boolean			= false

	private var loopSpan:Option[Span]	= None
	private var loopDef:Option[LoopDef]	= None

	private var filterModeOld	= Player.filterOff
	private var biquadCoeffs	= Player.passCoeffs

	// (0+filterLow)..(nyquist-filterHigh), predivided by outputRate
	private val filterLow	= log2(Config.filterLow / outputRate)
	private val filterHigh	= log2(1.0 / 2.0 - Config.filterHigh / outputRate)
	private val filterSize	= filterHigh - filterLow

	//------------------------------------------------------------------------------
	//## engine api

	private[player] def isRunning:Boolean	= running

	private[player] def feedback:PlayerFeedback	=
		PlayerFeedback(
			running			= running,
			afterEnd		= afterEnd,
			position		= headFrame,
			pitch			= pitch,
			measureMatch	= phaseMatch(RhythmUnit.Measure),
			beatRate		= beatRate,
			needSync		= needSync,
			hasSync			= hasSync,
			// NOTE this resets the peak detector
			masterPeak		= peakDetector.decay,
			loopSpan		= loopSpan,
			loopDef			= loopDef
		)

	private[player] def react(action:PlayerAction):Unit	=
		action match {
			case c@PlayerAction.ChangeControl(_,_,_,_,_,_,_)	=> doChangeControl(c)
			case PlayerAction.SetNeedSync(needSync)			=> doSetNeedSync(needSync)
			case PlayerAction.SetFile(file)					=> doSetFile(file)
			case PlayerAction.SetRhythm(rhythm)				=> doSetRhythm(rhythm)
			case PlayerAction.SetRunning(running)				=> doSetRunning(running)
			case PlayerAction.PitchAbsolute(pitch, keepSync)	=> doPitchAbsolute(pitch, keepSync)
			case PlayerAction.PhaseAbsolute(position)			=> doPlayerPhaseAbsolute(position)
			case PlayerAction.PhaseRelative(offset)			=> doPlayerPhaseRelative(offset)
			case PlayerAction.PositionAbsolute(frame)			=> doPlayerPositionAbsolute(frame)
			case PlayerAction.PositionJump(frame, rhythmUnit)	=> doPlayerPositionJump(frame, rhythmUnit)
			case PlayerAction.PositionSeek(offset)			=> doPlayerPositionSeek(offset:RhythmValue)
			case PlayerAction.DragAbsolute(v)					=> doDragAbsolute(v)
			case PlayerAction.DragEnd							=> doDragEnd()
			case PlayerAction.ScratchRelative(frames)			=> doScratchRelative(frames)
			case PlayerAction.ScratchEnd						=> doScratchEnd()
			case PlayerAction.LoopEnable(size)				=> doLoopEnable(size)
			case PlayerAction.LoopDisable						=> doLoopDisable()
		}

	//------------------------------------------------------------------------------
	//## common control

	// TODO ugly abuse of a ctor as a type
	private def doChangeControl(control:PlayerAction.ChangeControl):Unit	= {
		trim	.target(control.trim)
		filter	.target(control.filter)
		low		.target(control.low)
		middle	.target(control.middle)
		high	.target(control.high)
		speaker	.target(control.speaker)
		phone	.target(control.phone)
	}

	private def doSetFile(file:Option[Path]):Unit	= {
		file match {
			case Some(file)	=> loaderDecode(file)
			case None		=> setSample(None)
		}
	}

	private def setSample(sample:Option[CacheSample]):Unit	= {
		this.sample	= sample

		inputL	= sample.cata(Channel.empty, _.channelOrEmpty(0))
		inputR	= sample.cata(Channel.empty, _.channelOrEmpty(1))
		rate	= sample.cata(1, _.frameRate.toDouble)

		doLoopDisable()
		updateVelocity()
		updateEndFrame()
		keepSpeedSynced()
	}

	private def doSetRhythm(rhythm:Option[Rhythm]):Unit	= {
		this.rhythm	= rhythm

		updateEndFrame()
		keepSpeedSynced()
	}

	private def doSetNeedSync(needSync:Boolean):Unit	= {
		if (needSync == this.needSync)	return
		this.needSync	= needSync

		keepSpeedSynced()
	}

	//------------------------------------------------------------------------------
	//## registered fades

	private def doPlayerPhaseAbsolute(position:RhythmValue):Unit	= {
		registerSimpleFade {
			syncPhaseTo(position)
		}
	}

	private def doPlayerPhaseRelative(offset:RhythmValue):Unit	= {
		registerSimpleFade {
			movePhaseBy(offset)
		}
	}

	private def doPlayerPositionAbsolute(frame:Double):Unit	= {
		registerJump {
			loaderPreload(frame)
			loaderNotifyEngine {
				registerCancellableFade(
					{
						positionAbsolute(frame)
						exitJump()
					},
					exitJump()
				)
			}
		}
	}

	private def doPlayerPositionJump(frame:Double, rhythmUnit:RhythmUnit):Unit	= {
		registerJump {
			loaderPreload(frame)
			loaderNotifyEngine {
				registerCancellableFade(
					{
						positionJump(frame, rhythmUnit)
						exitJump()
					},
					exitJump()
				)
			}
		}
	}

	private def doPlayerPositionSeek(offset:RhythmValue):Unit	= {
		registerJump {
			// TODO loader questionable
			val estimateTarget	= headFrame + (rhythm.getOrElse(fakeRhythm).sizeOf(offset))
			loaderPreload(estimateTarget)
			loaderNotifyEngine {
				registerCancellableFade(
					{
						positionSeek(offset)
						exitJump()
					},
					exitJump()
				)
			}
		}
	}

	//------------------------------------------------------------------------------
	//## motor running

	private def doSetRunning(running:Boolean):Unit	= {
		val oldPhase	= phaseValue(RhythmUnit.Measure)

		this.running	= running
		updateVelocity()

		// keep phase stable over start/stop
		if (needSync && canSync && mode == PlayerMode.Playing) {
			oldPhase.foreach(syncPhaseTo)
		}

		killFade()
	}

	//------------------------------------------------------------------------------
	//## beat rate

	/** called whenever the Metronome has changed its beatRate value */
	private [player] def metronomeBeatRateChanged():Unit	= {
		keepSpeedSynced()
	}

	/** beats per second */
	private def beatRate:Option[Double]	=
		rhythm.map { rhythmGot =>
			pitch * rate / rhythmGot.beat
		}

	//------------------------------------------------------------------------------
	//## motor speed

	private def doPitchAbsolute(pitch:Double, keepSync:Boolean):Unit	=  {
		if (this.pitch == pitch)	return
		this.pitch	= pitch
		updateVelocity()

		needSync	&= keepSync
	}

	//------------------------------------------------------------------------------
	//## sync

	private def hasSync:Boolean	=
		needSync && canSync

	private def canSync:Boolean	=
		sample.isDefined &&
		rhythm.isDefined

	// depends on metronome.beatRate, beatRate (sample&rhythm) and needSync
	private def keepSpeedSynced():Unit	= {
		if (!hasSync)	return
		/*
		beatRate.foreach { beatRateGot =>
			pitch	= pitch * metronome.beatRate / beatRateGot
			updateVelocity()
		}
		*/
		rhythm.foreach { rhythmGot =>
			pitch	= metronome.beatRate * rhythmGot.beat / rate
			updateVelocity()
		}
	}

	//------------------------------------------------------------------------------
	//## phase

	/** set the phase to an absolute value */
	private def syncPhaseTo(position:RhythmValue):Unit	= {
		phaseMatch(position.unit).foreach { phaseGot =>
			movePhaseBy(position.move(-phaseGot))
		}
	}

	/** change the phase by some offset */
	private def movePhaseBy(offset:RhythmValue):Unit	= {
		rhythm.map(_.sizeOf(offset)).foreach(moveInLoop)
		/*
		currentRhythmRaster(offset.unit).foreach { raster =>
			moveInLoop(offset.steps * raster.size)
		}
		*/
	}

	private def phaseValue(rhythmUnit:RhythmUnit):Option[RhythmValue]	=
		phaseMatch(rhythmUnit).map(RhythmValue(_, rhythmUnit))

	private def phaseMatch(rhythmUnit:RhythmUnit):Option[Double]	=
		if (running)	phaseMetronome(rhythmUnit)
		else			phaseStatic(rhythmUnit)

	/** how far we are from the rhythm of the Metronome in [-.5..+.5] rhythmUnits for late to early */
	private def phaseMetronome(rhythmUnit:RhythmUnit):Option[Double]	=
		currentRhythmPhase(rhythmUnit).map { here =>
			val there	= metronome.phase(rhythmUnit)
			moduloDouble(here - there + 0.5, 1) - 0.5
		}

	/** how far we are from the track beat in [-.5..+.5] rhythmUnits for late to early */
	private def phaseStatic(rhythmUnit:RhythmUnit):Option[Double]	=
		currentRhythmPhase(rhythmUnit).map { here =>
			moduloDouble(here + 0.5, 1) - 0.5
		}

	private def currentRhythmPhase(rhythmUnit:RhythmUnit):Option[Double]	=
		currentRhythmRaster(rhythmUnit).map(_.phase(headFrame))

	private def currentRhythmRaster(rhythmUnit:RhythmUnit):Option[Raster]	=
		rhythm.map(_.raster(rhythmUnit))

	//------------------------------------------------------------------------------
	//## motor position

	private def positionAbsolute(frame:Double):Unit	= {
		startFade(frame)
	}

	/** jump to a given position without while staying in sync  */
	private def positionJump(frame:Double, rhythmUnit:RhythmUnit):Unit	= {
		rhythm match {
			case Some(rhythm)	=> positionJumpWithRaster(frame, rhythmUnit, rhythm)
			case None			=> positionAbsolute(frame)
		}
	}

	/** jump to a given position without while staying in sync  */
	private def positionJumpWithRaster(frame:Double, unit:RhythmUnit, rhythm:Rhythm):Unit	= {
		// TODO seek ugly: raster is calculated again in positionSeekWithRaster
		val raster	= rhythm.raster(unit)
		val raw		= (frame - headFrame) / raster.size
		val steps	= if (running) rint(raw) else raw
		positionSeekWithRaster(RhythmValue(steps, unit), rhythm)
	}

	/** jump for a given number of rhythm while staying in sync */
	private def positionSeek(offset:RhythmValue):Unit	= {
		rhythm match {
			case Some(rhythm)	=> positionSeekWithRaster(offset, rhythm)
			case None			=> positionSeekWithoutRaster(offset)
		}
	}

	private def positionSeekWithoutRaster(offset:RhythmValue):Unit	= {
		val position	= headFrame + fakeRhythm.sizeOf(offset)
		startFade(position)
	}

	/** jump for a given number of rhythm while staying in sync */
	private def positionSeekWithRaster(offset:RhythmValue, rhythm:Rhythm):Unit	= {
		val position:Double		=
			if (running)	headFrame + rhythm.sizeOf(offset)
			else			snappingSeekCalculation(headFrame, offset.steps, rhythm.raster(offset.unit))
		startFade(position)
	}

	/** almost ceil for negative offsets, almost floor for positive offsets */
	@nowarn("msg=unused explicit parameter")
	private def snappingSeekCalculation(start:Double, steps:Double, raster:Raster):Double	= {
		val	offset	= (0.5 - Player.positionEpsilon) * signum(steps)
		val	raw		= headFrame + (steps - offset) * raster.size
		raster.round(raw)
		/*
		// equivalent code:
		if (steps < 0)	raster ceil		(headFrame + (steps - Player.positionEpsilon) * raster.size)
		else			raster floor	(headFrame + (steps + Player.positionEpsilon) * raster.size)
		*/
	}

	// TODO raster ugly
	private  def fakeRhythm	= Rhythm.fake(0, rate)

	//------------------------------------------------------------------------------
	//## motor scratch

	// motor -> scratch
	private def scratchBegin():Unit	= {
		fadeLater			= None
		mode				= PlayerMode.Scratching
		scratchBaseFrame	= headFrame
		springOriginFrame	= headFrame
		acceleration		= 0
		velocity			= 0
	}

	private def doScratchRelative(frames:Double):Unit	= {
		if (mode != PlayerMode.Scratching)	{
			scratchBegin()
		}
		springOriginFrame	= scratchBaseFrame - frames
	}

	// scratch -> motor
	private def doScratchEnd():Unit	= {
		mode	= PlayerMode.Playing
		updateVelocity()
	}

	//------------------------------------------------------------------------------
	//## motor drag

	// motor -> scratch
	private def dragBegin():Unit	= {
		mode	= PlayerMode.Dragging
	}

	private def doDragAbsolute(speed:Double):Unit	= {
		if (mode != PlayerMode.Dragging) {
			dragBegin()
		}
		velocity	= speed * rate
	}

	// scratch -> motor
	private def doDragEnd():Unit	= {
		mode	= PlayerMode.Playing
		updateVelocity()
	}

	//------------------------------------------------------------------------------
	//## delayed jumping

	private def registerJump(block: =>Unit):Unit	= {
		jumpLater	= Some(thunk(block))
	}

	private def enterJump():Unit	= {
		if (!jumpProgress) {
			jumpLater.foreach { it =>
				it.apply()
				jumpProgress	= true
				jumpLater		= None
			}
		}
	}

	private def exitJump():Unit	= {
		jumpProgress	= false
	}

	//------------------------------------------------------------------------------
	//## loader interaction

	private[player] def talkToLoader():Unit	= {
		enterJump()

		loaderPreload(headFrame)

		if (fadeProgress) {
			loaderPreload(fadeFrame)
		}

		// TODO fade only send when loopSpan changes
		loopSpan.foreach { it =>
			loaderPreload(it.start)
		}
	}

	private def loaderDecode(file:Path):Unit	= {
		loaderTarget.send(LoaderAction.Decode(file, setSample))
	}

	private def loaderPreload(centerFrame:Double):Unit	= {
		sample.foreach { it =>
			loaderTarget.send(LoaderAction.Preload(it, centerFrame.toInt))
		}
	}

	private def loaderNotifyEngine(block: =>Unit):Unit	= {
		loaderTarget.send(LoaderAction.NotifyEngine(thunk(block)))
	}

	//------------------------------------------------------------------------------
	//## fading

	private trait Fade {
		def execute():Unit
		def cancel():Unit
	}

	private def registerCancellableFade(executeBlock: =>Unit, cancelBlock: =>Unit):Unit	= {
		registerFadeImpl(new Fade {
			def execute():Unit	= { executeBlock }
			def cancel():Unit	= { cancelBlock}
		})
	}

	private def registerSimpleFade(executeBlock: =>Unit):Unit	= {
		registerFadeImpl(new Fade {
			def execute():Unit	= { executeBlock }
			def cancel():Unit	= {}
		})
	}

	private def registerFadeImpl(fade:Fade):Unit	= {
		fadeLater.foreach(
			_.cancel()
		)
		fadeLater	= Some(fade)
	}

	private def killFade():Unit	= {
		fadeProgress	= false
		fadeLater		= None
	}

	/** all calls to this must be routed though registerFade */
	private def startFade(newFrame:Double):Unit	= {
		fadeFrame		= headFrame
		headFrame		= newFrame
		fadeValue		= fadeMin
	}

	@inline
	private def enterFade():Unit	= {
		if (!fadeProgress) {
			fadeLater.foreach { it =>
				// this calls back to startFade
				it.execute()
				fadeProgress	= true
				fadeLater		= None
			}
		}
	}

	@inline
	private def exitFade():Unit	= {
		if (fadeProgress) {
			fadeValue	+= fadeStep
			if (fadeValue >= fadeMax) {
				fadeProgress	= false
			}
		}
	}

	//------------------------------------------------------------------------------
	//## filter calculation

	// value in 0..1
	@inline
	private def filterFreq(offset:Double):Double	=
		exp2(offset * filterSize + filterLow)

	// returns a filter mode
	@inline
	private def filterMode(value:Double):Int	=
		if		(value < -Player.filterEpsilon)	Player.filterLP
		else if	(value > +Player.filterEpsilon)	Player.filterHP
		else									Player.filterOff

	//------------------------------------------------------------------------------
	//## loop calculation

	private def doLoopEnable(preset:LoopDef):Unit	= {
		loopSpan	= mkLoop(headFrame, preset.size)
		loopDef		= loopSpan.isDefined.option(preset)
	}

	private def doLoopDisable():Unit	= {
		loopSpan	= None
		loopDef		= None
	}

	// prevent loops being set a hole raster roo early when we're too close to the edge
	private val loopShift	= {
		val vsa = 1.0 / 4294967295.0
		2*vsa
	}

	// start is rastered by size.unit only. not scaled
	private def mkLoop(frame:Double, size:RhythmValue):Option[Span]	=
		currentRhythmRaster(size.unit).map { raster =>
			Span(
				raster.floor(frame+loopShift),
				raster.size * size.steps
			)
		}

	/*
	// moves a loop we're in around the new position
	private def loopAround(position:Double) {
		loopSpan	=
			loopSpan.map { it =>
				if (it contains x)	it move (position - x)
				else				it
			}
	}
	*/

	private def moveInLoop(offset:Double):Unit	= {
		val rawFrame	= headFrame + offset
		val newFrame	=
			loopSpan match {
				case Some(loopGot) =>
					if (loopGot.contains(headFrame)) {
						loopGot.lock(rawFrame)
					}
					else rawFrame
				case None	=>
					rawFrame
			}
		startFade(newFrame)
	}

	private def jumpBackAfterLoopEnd(oldFrame:Double):Unit	= {
		loopSpan.foreach { loopGot =>
			val loopEnd	= loopGot.end
			if (oldFrame < loopEnd && headFrame >= loopEnd) {
				registerSimpleFade {
					startFade(headFrame - loopGot.size)
				}
			}
		}
	}

	//------------------------------------------------------------------------------
	//## generator

	// depends on running, mode, pitch, rate
	private def updateVelocity():Unit	= {
		if (mode != PlayerMode.Scratching) {
			velocity	= if (running) pitch * rate else 0
		}
	}

	private def updateEndFrame():Unit	= {
		val frameCount	=
			sample.cata(0, _.frameCount)
		endFrame	=
			rhythm.cata(
				frameCount + outputRate*Player.endDelay,
				_.measureRaster.ceil(frameCount)
			)
	}

	def generate(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer):Unit	= {
		// NOTE hack
		if (running && mode == PlayerMode.Playing && afterEnd) {
			doSetRunning(false)
		}

		enterFade()

		// current head
		val headSpeed	= abs(velocity) * deltaTime
		val fadeinL		= Player.interpolation.interpolate(inputL, headFrame, headSpeed)
		val fadeinR		= Player.interpolation.interpolate(inputR, headFrame, headSpeed)

		// optionally fade out pre-jump head
		var audioL	= 0d
		var audioR	= 0d
		if (fadeProgress) {
			val fadeoutL	= Player.interpolation.interpolate(inputL, fadeFrame, headSpeed)
			val fadeoutR	= Player.interpolation.interpolate(inputR, fadeFrame, headSpeed)
			audioL	= fadeinL * fadeValue + fadeoutL * (1-fadeValue)
			audioR	= fadeinR * fadeValue + fadeoutR * (1-fadeValue)
		}
		else {
			audioL	= fadeinL
			audioR	= fadeinR
		}

		// equalizer
		val lowValue	= low.current
		val middleValue	= middle.current
		val highValue	= high.current
		val equalizedL	= equalizerL.process(audioL, lowValue, middleValue, highValue)
		val equalizedR	= equalizerR.process(audioR, lowValue, middleValue, highValue)

		// filter
		val filterValue		= filter.current
		// reset filter when the mode changed
		val filterModeNow	= filterMode(filterValue)
		if (filterModeNow != filterModeOld) {
			filterModeOld	= filterModeNow

			filterL.reset()
			filterR.reset()
		}
		// NOTE this only has to be done when filterValue changed,
		// but moving this inside an if-block to change actually kills performance
		// recalculate coeffients when the mode or frequency changed
		if (filterModeNow == Player.filterLP) {
			val freq		= filterFreq(filterValue + 1)
			biquadCoeffs	= BiQuadCoeffs.lp(freq, Config.filterQ)
		}
		else if (filterModeNow == Player.filterHP) {
			val freq		= filterFreq(filterValue + 0)
			biquadCoeffs	= BiQuadCoeffs.hp(freq, Config.filterQ)
		}
		else {
			biquadCoeffs	= Player.passCoeffs
		}

		val filteredL	= filterL.process(equalizedL, biquadCoeffs)
		val filteredR	= filterR.process(equalizedR, biquadCoeffs)

		// NOTE hack: output only when moving
		if (velocity != zeroFrequency) {	// playing || scratchMode
			val trimGain		= trim.current
			val speakerValue	= speaker.current
			val combinedGain	= speakerValue * trimGain
			val speakerL		= (filteredL * combinedGain).toFloat
			val speakerR		= (filteredR * combinedGain).toFloat

			speakerBuffer.add(speakerL, speakerR)

			peakDetector.put(speakerL)
			peakDetector.put(speakerR)

			if (phoneEnabled) {
				val phoneValue	= phone.current
				val phoneL		= (audioL * phoneValue).toFloat
				val phoneR		= (audioR * phoneValue).toFloat

				phoneBuffer.add(phoneL, phoneR)
			}
		}

		if (mode == PlayerMode.Scratching) {
			val	c				= Player.springDamping * 2 * sqrt(Player.springHardness * Player.springMass)
			// spring is assumed to have zero length when not stretched
			val springStretch	= headFrame - springOriginFrame
			acceleration		= DoubleUtil.ftz((-Player.springHardness*springStretch - c*velocity) / Player.springMass)
			val v1				= velocity + acceleration * deltaTime
			velocity			= clampDouble(v1, -springSpeedLimit, springSpeedLimit)
		}

		val oldFrame	= headFrame
		val deltaFrame	= velocity * deltaTime
		headFrame		+= deltaFrame
		fadeFrame		+= deltaFrame

		exitFade()

		if (mode == PlayerMode.Playing) {
			jumpBackAfterLoopEnd(oldFrame)
		}

		trim.step()
		filter.step()
		low.step()
		middle.step()
		high.step()
		speaker.step()
		phone.step()
	}
}
