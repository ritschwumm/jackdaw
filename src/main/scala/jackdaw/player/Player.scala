package jackdaw.player

import java.io.File

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.math._

import scaudio.control._
import scaudio.output._
import scaudio.sample._
import scaudio.interpolation._
import scaudio.math._
import scaudio.dsp._

import jackdaw._
import jackdaw.data._
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
	
	private val interpolation	= Config.sincEnabled cata (Linear, Sinc)
	
	// in frames
	 val maxDistance	= interpolation overshot springPitchLimit
	
	 // headFrame, fadeFrame, jump, loop
	 val headCount		= 4
}

/**
one audio line outputting audio data for one Deck using a MixHalf
public methods must never be called outside the engine thread
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
	private val trim		= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	// -1..+1
	private val filter		= DamperDouble forRates (0.0,		Player.dampTime, outputRate)
	// 0..1 range
	private val low			= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	private val middle		= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	private val high		= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	// 0..1 range
	private val speaker		= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	private val phone		= DamperDouble forRates (unitGain,	Player.dampTime, outputRate)
	
	private val peakDetector	= new PeakDetector
	
	private var mode:PlayerMode	= Playing
	
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
	
	private var jumpLater:Option[Task]	= None
	private var jumpProgress:Boolean	= false
	
	private var loopSpan:Option[Span]	= None
	private var loopDef:Option[LoopDef]	= None
	
	private var filterModeOld	= Player.filterOff
	
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
				measureMatch	= phaseMatch(Measure),
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
				case c@PlayerChangeControl(_,_,_,_,_,_,_)	=> changeControl(c)
				
				case PlayerSetNeedSync(needSync)			=> setNeedSync(needSync)
				
				case PlayerSetFile(file)					=>
						file match {
							case Some(file)	=> loaderDecode(file)
							case None		=> setSample(None)
						}
				case PlayerSetRhythm(rhythm)				=> setRhythm(rhythm)
				
				case PlayerSetRunning(running)				=> setRunning(running)
				
				case PlayerPitchAbsolute(pitch, keepSync)	=> pitchAbsolute(pitch, keepSync)
				
				case PlayerPhaseAbsolute(position)			=>
					registerSimpleFade {
						syncPhaseTo(position)
					}
				case PlayerPhaseRelative(offset)			=>
					registerSimpleFade {
						movePhaseBy(offset)
					}
				
				case PlayerPositionAbsolute(frame)			=>
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
				case PlayerPositionJump(frame, rhythmUnit)	=>
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
				case PlayerPositionSeek(offset)				=>
					registerJump {
						// TODO loader questionable
						loaderPreload(headFrame + offset.steps * jumpRaster(offset).size)
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
				
				case PlayerDragAbsolute(v)					=> dragAbsolute(v)
				case PlayerDragEnd							=> dragEnd()
				
				case PlayerScratchRelative(frames)			=> scratchRelative(frames)
				case PlayerScratchEnd						=> scratchEnd()
				
				case PlayerLoopEnable(size)					=> loopEnable(size)
				case PlayerLoopDisable						=> loopDisable()
			}
			
	//------------------------------------------------------------------------------
	//## common control
	
	private def changeControl(control:PlayerChangeControl) {
		trim	target	control.trim
		filter	target	control.filter
		low		target	control.low
		middle	target	control.middle
		high	target	control.high
		speaker	target	control.speaker
		phone	target	control.phone
	}

	private def setSample(sample:Option[CacheSample]) {
		this.sample	= sample
		
		inputL	= sample cata (Channel.empty, _ channelOrEmpty 0)
		inputR	= sample cata (Channel.empty, _ channelOrEmpty 1)
		rate	= sample cata (1, _.frameRate.toDouble)
		
		loopDisable()
		updateVelocity()
		updateEndFrame()
		keepSpeedSynced()
	}
	
	private def setRhythm(rhythm:Option[Rhythm]) {
		this.rhythm	= rhythm
		
		updateEndFrame()
		keepSpeedSynced()
	}
	
	private def setNeedSync(needSync:Boolean) {
		if (needSync == this.needSync)	return
		this.needSync	= needSync
		
		keepSpeedSynced()
	}
	
	//------------------------------------------------------------------------------
	//## motor running
	
	private def setRunning(running:Boolean) {
		val oldPhase	= phaseValue(Measure)
		
		this.running	= running
		updateVelocity()
		
		// keep phase stable over start/stop
		if (needSync && canSync && mode == Playing) {
			oldPhase foreach syncPhaseTo
		}
			
		killFade()
	}
		
	//------------------------------------------------------------------------------
	//## beat rate
	
	/** called whenever the Metronome has changed its beatRate value */
	private [player] def metronomeBeatRateChanged() {
		keepSpeedSynced()
	}
	
	/** beats per second */
	private def beatRate:Option[Double]	=
			rhythm map { rhythmGot =>
				pitch * rate / rhythmGot.beat
			}
			
	//------------------------------------------------------------------------------
	//## motor speed
	
	private def pitchAbsolute(pitch:Double, keepSync:Boolean)  {
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
	private def keepSpeedSynced() {
		if (!hasSync)	return
		beatRate foreach { beatRateGot =>
			pitch	= pitch * metronome.beatRate / beatRateGot
			updateVelocity()
		}
	}
	
	//------------------------------------------------------------------------------
	//## phase
	
	/** set the phase to an absolute value */
	private def syncPhaseTo(position:RhythmValue) {
		phaseMatch(position.unit) foreach { phaseGot =>
			movePhaseBy(position move -phaseGot)
		}
	}
			
	/** change the phase by some offset */
	private def movePhaseBy(offset:RhythmValue) {
		currentRhythmRaster(offset.unit) foreach { raster =>
			moveInLoop(offset.steps * raster.size)
		}
	}
	
	private def phaseValue(rhythmUnit:RhythmUnit):Option[RhythmValue]	=
			phaseMatch(rhythmUnit) map { RhythmValue(_, rhythmUnit) }
		
	private def phaseMatch(rhythmUnit:RhythmUnit):Option[Double]	=
			if (running)	phaseMetronome(rhythmUnit)
			else			phaseStatic(rhythmUnit)
		
	/** how far we are from the rhythm of the Metronome in [-.5..+.5] rhythmUnits for late to early */
	private def phaseMetronome(rhythmUnit:RhythmUnit):Option[Double]	=
			currentRhythmPhase(rhythmUnit) map { here =>
				val there	= metronome	phase rhythmUnit
				moduloDouble(here - there + 0.5, 1) - 0.5
			}
			
	/** how far we are from the track beat in [-.5..+.5] rhythmUnits for late to early */
	private def phaseStatic(rhythmUnit:RhythmUnit):Option[Double]	=
			currentRhythmPhase(rhythmUnit) map { here =>
				moduloDouble(here + 0.5, 1) - 0.5
			}
			
	private def currentRhythmPhase(rhythmUnit:RhythmUnit):Option[Double]	=
			currentRhythmRaster(rhythmUnit) map { _ phase headFrame }
			
	private def currentRhythmRaster(rhythmUnit:RhythmUnit):Option[Raster]	=
			rhythm map { _ raster rhythmUnit }
			
	//------------------------------------------------------------------------------
	//## motor position
	
	private def positionAbsolute(frame:Double) {
		startFade(frame)
	}
	
	/** jump to a given position without while staying in sync  */
	private def positionJump(frame:Double, rhythmUnit:RhythmUnit) {
		rhythm match {
			case Some(rhythm)	=> positionJumpWithRaster(frame, rhythm raster rhythmUnit)
			case None			=> positionAbsolute(frame)
		}
	}
	
	/** jump to a given position without while staying in sync  */
	private def positionJumpWithRaster(frame:Double, raster:Raster) {
		val raw		= (frame - headFrame) / raster.size
		val steps	= if (running) rint(raw) else raw
		positionSeekWithRaster(steps, raster)
	}
	
	/** jump for a given number of rhythm while staying in sync */
	private def positionSeek(offset:RhythmValue) {
		positionSeekWithRaster(offset.steps, jumpRaster(offset))
	}
	
	/** jump for a given number of rhythm while staying in sync */
	private def positionSeekWithRaster(steps:Double, raster:Raster) {
		val position:Double		=
				if (running) {
					 headFrame + steps * raster.size
				}
				else {
					val	offset 	= (0.5 - Player.positionEpsilon) * signum(steps)
					val	raw		= headFrame + (steps - offset) * raster.size
					raster round raw
				}
		// loopAround(position)
		startFade(position)
	}
	
	private def jumpRaster(offset:RhythmValue):Raster	=
			rhythm getOrElse fakeRhythm raster offset.unit
		
	// TODO raster ugly
	private  def fakeRhythm	= Rhythm fake (0, rate)
	
	//------------------------------------------------------------------------------
	//## motor scratch
	
	// motor -> scratch
	private def scratchBegin() {
		fadeLater			= None
		mode				= Scratching
		scratchBaseFrame	= headFrame
		springOriginFrame	= headFrame
		acceleration		= 0
		velocity			= 0
	}
	
	private def scratchRelative(frames:Double) {
		if (mode != Scratching)	{
			scratchBegin()
		}
		springOriginFrame	= scratchBaseFrame - frames
	}
	
	// scratch -> motor
	private def scratchEnd() {
		mode	= Playing
		updateVelocity()
	}
	
	//------------------------------------------------------------------------------
	//## motor drag
	
	// motor -> scratch
	private def dragBegin() {
		mode	= Dragging
	}
	
	private def dragAbsolute(speed:Double) {
		if (mode != Dragging) {
			dragBegin()
		}
		velocity	= speed * rate
	}
	
	// scratch -> motor
	private def dragEnd() {
		mode	= Playing
		updateVelocity()
	}
	
	//------------------------------------------------------------------------------
	//## delayed jumping
	
	private def registerJump(block: =>Unit) {
		jumpLater	= Some(thunk(block))
	}
	
	private def enterJump() {
		if (!jumpProgress && jumpLater.isDefined) {
			jumpLater.get.apply()
			jumpProgress	= true
			jumpLater		= None
		}
	}
	
	private def exitJump() {
		jumpProgress	= false
	}
	
	//------------------------------------------------------------------------------
	//## loader interaction
	
	private[player] def talkToLoader() {
		enterJump()
		
		loaderPreload(headFrame)
		
		if (fadeProgress) {
			loaderPreload(fadeFrame)
		}
		
		// TODO fade only send when loopSpan changes
		if (loopSpan.isDefined) {
			loaderPreload(loopSpan.get.start)
		}
	}
	
	private def loaderDecode(file:File) {
		loaderTarget send LoaderDecode(file, setSample)
	}

	private def loaderPreload(centerFrame:Double) {
		if (sample.isDefined) {
			loaderTarget send LoaderPreload(sample.get, centerFrame.toInt)
		}
	}
	
	private def loaderNotifyEngine(block: =>Unit) {
		loaderTarget send LoaderNotifyEngine(thunk(block))
	}
	
	//------------------------------------------------------------------------------
	//## fading
	
	private trait Fade {
		def execute():Unit
		def cancel():Unit
	}
	
	private def registerCancellableFade(executeBlock: =>Unit, cancelBlock: =>Unit) {
		registerFadeImpl(new Fade {
			def execute() { executeBlock }
			def cancel() { cancelBlock}
		})
	}
	
	private def registerSimpleFade(executeBlock: =>Unit) {
		registerFadeImpl(new Fade {
			def execute() { executeBlock }
			def cancel() {}
		})
	}
	
	private def registerFadeImpl(fade:Fade) {
		if (fadeLater.isDefined) {
			fadeLater.get.cancel()
		}
		fadeLater	= Some(fade)
	}
	
	private def killFade() {
		fadeProgress	= false
		fadeLater		= None
	}
	
	/** all calls to this must be routed though registerFade */
	private def startFade(newFrame:Double) {
		fadeFrame		= headFrame
		headFrame		= newFrame
		fadeValue		= fadeMin
	}
	
	@inline
	private def enterFade() {
		if (!fadeProgress && fadeLater.isDefined) {
			// this calls back to startFade
			fadeLater.get.execute()
			fadeProgress	= true
			fadeLater		= None
		}
	}
	
	@inline
	private def exitFade() {
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
				 if (value < -Player.filterEpsilon)	Player.filterLP
			else if (value > +Player.filterEpsilon)	Player.filterHP
			else									Player.filterOff
			
	//------------------------------------------------------------------------------
	//## loop calculation
	
	private def loopEnable(preset:LoopDef) {
		loopSpan	= mkLoop(headFrame, preset.size)
		loopDef		= loopSpan.isDefined guard preset
	}
	
	private def loopDisable() {
		loopSpan	= None
		loopDef		= None
	}
	
	// start is rastered by size.unit only. not scaled
	private def mkLoop(frame:Double, size:RhythmValue):Option[Span]	=
			currentRhythmRaster(size.unit) map { raster =>
				Span(raster floor frame, raster.size * size.steps)
			}
		
	/*
	// moves a loop we're in around the new position
	private def loopAround(position:Double) {
		loopSpan	= loopSpan map { it =>
			if (it contains x)	it move (position - x)
			else				it
		}
	}
	*/
	
	private def moveInLoop(offset:Double) {
		val rawFrame	= headFrame + offset
		val newFrame	=
				if (loopSpan.isDefined) {
					val loopGot	= loopSpan.get
					if (loopGot contains headFrame) {
						loopGot lock rawFrame
					}
					else rawFrame
				}
				else rawFrame
		startFade(newFrame)
	}
	
	private def jumpBackAfterLoopEnd(oldFrame:Double) {
		if (loopSpan.isDefined) {
			val loopGot	= loopSpan.get
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
	private def updateVelocity() {
		if (mode != Scratching) {
			velocity	= if (running) pitch * rate else 0
		}
	}
	
	private def updateEndFrame() {
		val frameCount	=
				sample cata (0, _.frameCount)
		endFrame	=
				rhythm cata (
					frameCount + outputRate*Player.endDelay,
					_.measureRaster ceil frameCount
				)
	}
	
	def generate(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer) {
		// NOTE hack
		if (running && mode == Playing && afterEnd) {
			setRunning(false)
		}
		
		enterFade()
		
		// current head
		val headSpeed	= abs(velocity) * deltaTime
		val fadeinL		= Player.interpolation interpolate (inputL, headFrame, headSpeed)
		val fadeinR		= Player.interpolation interpolate (inputR, headFrame, headSpeed)
		
		// optionally fade out pre-jump head
		var audioL	= 0d
		var audioR	= 0d
		if (fadeProgress) {
			val fadeoutL	= Player.interpolation interpolate (inputL, fadeFrame, headSpeed)
			val fadeoutR	= Player.interpolation interpolate (inputR, fadeFrame, headSpeed)
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
		val equalizedL	= equalizerL process (audioL, lowValue, middleValue, highValue)
		val equalizedR	= equalizerR process (audioR, lowValue, middleValue, highValue)
		
		// filter
		val filterValue		= filter.current
		// reset filter when the mode changed
		val filterModeNow	= filterMode(filterValue)
		if (filterModeNow != filterModeOld) {
			filterModeOld	= filterModeNow
			filterL.reset()
			filterR.reset()
		}
		// calculate filter only in active mode
		var filteredL	= 0.0
		var filteredR	= 0.0
		if (filterModeNow == Player.filterLP) {
			val freq	= filterFreq(filterValue + 1)
			val	coeffs	= BiQuadCoeffs lp (freq, Config.filterQ)
			filteredL	= filterL process (equalizedL, coeffs)
			filteredR	= filterR process (equalizedR, coeffs)
		}
		else if (filterModeNow == Player.filterHP) {
			val freq	= filterFreq(filterValue + 0)
			val coeffs	= BiQuadCoeffs hp (freq, Config.filterQ)
			filteredL	= filterL process (equalizedL, coeffs)
			filteredR	= filterR process (equalizedR, coeffs)
		}
		else {
			filteredL	= equalizedL
			filteredR	= equalizedR
		}
		
		// NOTE hack: output only when moving
		if (velocity != zeroFrequency) {	// playing || scratchMode
			val trimGain		= trim.current
			val speakerValue	= speaker.current
			val combinedGain	= speakerValue * trimGain
			val speakerL		= (filteredL * combinedGain).toFloat
			val speakerR		= (filteredR * combinedGain).toFloat
			
			speakerBuffer add (speakerL, speakerR)
			
			peakDetector put speakerL
			peakDetector put speakerR
			
			if (phoneEnabled) {
				val phoneValue	= phone.current
				val phoneL		= (audioL * phoneValue).toFloat
				val phoneR		= (audioR * phoneValue).toFloat
		
				phoneBuffer add (phoneL, phoneR)
			}
		}
		
		if (mode == Scratching) {
			val	c				= Player.springDamping * 2 * sqrt(Player.springHardness * Player.springMass)
			// spring is assumed to have zero length when not stretched
			val springStretch	= headFrame - springOriginFrame
			acceleration		= (-Player.springHardness*springStretch - c*velocity) / Player.springMass
			val v1	= velocity + acceleration * deltaTime
			velocity	= clampDouble(v1, -springSpeedLimit, springSpeedLimit)
		}
		
		val oldFrame	= headFrame
		val deltaFrame	= velocity * deltaTime
		headFrame		+= deltaFrame
		fadeFrame		+= deltaFrame
		
		exitFade()
		
		if (mode == Playing) {
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
