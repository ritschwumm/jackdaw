package jackdaw.player

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
	val dampTime	= 0.1	// 0..1 range in 1/10 of a second
	val fadeTime	= 0.02	// 20 ms
	val endDelay	= 0.1	// 100 ms
	val noEnd		= -1d
	
	// filter modes
	private val filterLP	= -1
	private val filterOff	= 0
	private val filterHP	= +1
	
	// ignore small aberrations
	private val	positionEpsilon		= 1.0E-4
	
	private val filterEpsilon		= 1.0E-5
	
	private	val springPitchLimit	= 100
	
	private val interpolation	= Config.sincEnabled cata (Linear, Sinc)
	
	// in frames
	val maxDistance	= interpolation overshot springPitchLimit
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
	
	private var sample:Option[Sample]	= None
	private var rhythm:Option[Rhythm]	= None
	private var rate:Double				= zeroFrequency
	private var inputL:Channel			= Channel.empty
	private var inputR:Channel			= Channel.empty
	
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
	
	private var mode:PlayerMode		= Playing
	private var scratchBase			= 0.0
	private val springSpeedLimit	= Player.springPitchLimit * outputRate
	private var endFrame			= Player.noEnd
	
	private var running		= false
	private var pitch		= unitFrequency
	private var needSync	= true
	
	private val fadeStep	= 1.0 / (Player.fadeTime * outputRate)
	private val fadeMin		= 0.0
	private val fadeMax		= 1.0
	private var fade		= fadeMax
	private var fadeLater:Option[Task]	= None
	
	private var loopSpan:Option[Span]	= None
	private var loopDef:Option[LoopDef]	= None
	
	//------------------------------------------------------------------------------
	//## engine api
	
	private[player] def isRunning:Boolean	= running
	
	private[player] def feedback:PlayerFeedback	= 
			PlayerFeedback(
				running			= running,
				afterEnd		= afterEnd,
				position		= x,
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
				
				case PlayerSetSample(sample)				=> setSample(sample)
				case PlayerSetRhythm(rhythm)				=> setRhythm(rhythm)
				
				case PlayerSetRunning(running)				=> setRunning(running)
				
				case PlayerPitchAbsolute(pitch, keepSync)	=> pitchAbsolute(pitch, keepSync)
				
				case PlayerPhaseAbsolute(position)			=> fadeNowOrLater { syncPhaseTo(position)	}
				case PlayerPhaseRelative(offset)			=> fadeNowOrLater { movePhaseBy(offset)		}
				
				case PlayerPositionAbsolute(frame)			=>
					loaderPreload(frame)
					loaderNotifyEngine(thunk {
						fadeNowOrLater { positionAbsolute(frame)	}
					})
				case PlayerPositionJump(frame, rhythmUnit)	=>
					loaderPreload(frame)
					loaderNotifyEngine(thunk {
						fadeNowOrLater { positionJump(frame, rhythmUnit)	}
					})
				case PlayerPositionSeek(offset)				=>
					// TODO loader questionable
					loaderPreload(x + offset.steps * jumpRaster(offset).size)
					loaderNotifyEngine(thunk {
						fadeNowOrLater { positionSeek(offset)	}
					})
				
				case PlayerDragAbsolute(v)					=> dragAbsolute(v)
				case PlayerDragEnd							=> dragEnd()
				
				case PlayerScratchRelative(frames)			=> scratchRelative(frames)
				case PlayerScratchEnd						=> scratchEnd()
				
				case PlayerLoopEnable(size)					=> loopEnable(size)
				case PlayerLoopDisable						=> loopDisable()
			}
			
	//------------------------------------------------------------------------------
	//## loader interaction
	
	private[player] def preloadCurrent() {
		loaderPreload(x)
		if (isFading) {
			loaderPreload(xf)
		}
		if (loopSpan.isDefined) {
			loaderPreload(loopSpan.get.start)
		}
	}
	
	// TODO should depend on BPM and loop size
	private val bufferFrames:Int	=
			ceil(Config.preloadSpread.millis * outputRate / 1000 + Player.maxDistance).toInt
		
	private def loaderPreload(centerFrame:Double) {
		if (sample.isDefined) {
			loaderTarget send LoaderPreload(sample.get, centerFrame.toInt, bufferFrames)
		}
	}
	
	private def loaderNotifyEngine(task:Task) {
		loaderTarget send LoaderNotifyEngine(task)
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

	private def setSample(sample:Option[Sample]) {
		this.sample	= sample
		
		inputL	= sample cata (Channel.empty, _ channelOrEmpty 0)
		inputR	= sample cata (Channel.empty, _ channelOrEmpty 1)
		rate	= sample cata (1, _.frameRate.toDouble)
		
		loopDisable()
		updateV()
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
	
	private def updateEndFrame() {
		val frameCount	=
				sample cata (0, _.frameCount)
		endFrame	=
				rhythm cata (
					frameCount + outputRate*Player.endDelay,
					_.measureRaster ceil frameCount
				)
	}
	
	//------------------------------------------------------------------------------
	//## motor running
	
	private def setRunning(running:Boolean) {
		val oldPhase	= phaseValue(Measure)
		
		this.running	= running
		updateV()
		
		// keep phase stable over start/stop
		if (needSync && canSync && mode == Playing) {
			oldPhase foreach syncPhaseTo
		}
			
		stopFade()
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
		updateV()
		
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
			updateV()
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
			currentRhythmRaster(rhythmUnit) map { _ phase x }
			
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
		val raw		= (frame - x) / raster.size
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
					 x + steps * raster.size
				}
				else {
					val	offset 	= (0.5 - Player.positionEpsilon) * signum(steps)
					val	raw		= x + (steps - offset) * raster.size
					raster round raw
				}
		// loopAround(position)
		startFade(position)
	}
	
	private def jumpRaster(offset:RhythmValue):Raster	=
			rhythm getOrElse fakeRhythm raster offset.unit
		
	// TODO raster ugly
	private  def fakeRhythm	= Rhythm simple (0, rate)
	
	//------------------------------------------------------------------------------
	//## motor scratch
	
	// motor -> scratch
	private def scratchBegin() {
		fadeLater	= None
		mode		= Scratching
		scratchBase	= x
		o			= x
		a			= 0
		v			= 0
	}
	
	private def scratchRelative(frames:Double) {
		if (mode != Scratching)	{
			scratchBegin()
		}
		o	= scratchBase - frames
	}
	
	// scratch -> motor
	private def scratchEnd() {
		mode	= Playing
		updateV()
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
		v	= speed * rate
	}
	
	// scratch -> motor
	private def dragEnd() {
		mode	= Playing
		updateV()
	}
	
	//------------------------------------------------------------------------------
	//## fading
	
	@inline
	private def isFading	= fade < fadeMax
	
	private def fadeNowOrLater(block: =>Unit) {
		if (isFading)	fadeLater	= Some(thunk { block })
		else			block
	}
	
	private def startFade(newX:Double) {
		xf		= x
		x		= newX
		fade	= fadeMin
	}
	
	@inline
	private def stopFade() {
		fade		= fadeMax
		fadeLater	= None
	}
	
	@inline
	private def doFade() {
		fade	+= fadeStep
		if (!isFading) {
			if (fadeLater.isDefined) {
				fade		= fadeMax
				fadeLater.get.apply()
				fadeLater	= None
			}
			else {
				stopFade()
			}
		}
	}
	
	//------------------------------------------------------------------------------
	//## filter calculation
	
	private var filterModeOld	= Player.filterOff
	
	// (0+filterLow)..(nyquist-filterHigh), predivided by outputRate
	private val filterLow	= log2(Config.filterLow / outputRate)
	private val filterHigh	= log2(1.0 / 2.0 - Config.filterHigh / outputRate)
	private val filterSize	= filterHigh - filterLow
	
	// value in 0..1
	private def filterFreq(offset:Double):Double	=
			exp2(offset * filterSize + filterLow)
		
	// returns a filter mode
	private def filterMode(value:Double):Int	=
				 if (value < -Player.filterEpsilon)	Player.filterLP
			else if (value > +Player.filterEpsilon)	Player.filterHP
			else									Player.filterOff
			
	//------------------------------------------------------------------------------
	//## loop calculation
	
	private def loopEnable(preset:LoopDef) {
		loopSpan	= mkLoop(x, preset.size)
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
		val rawX	= x + offset
		val newX	= 
				if (loopSpan.isDefined) {
					val loopGot	= loopSpan.get
					if (loopGot contains x) {
						loopGot lock rawX
					}
					else rawX
				}
				else rawX
		startFade(newX)
	}
	
	private def jumpBackAfterLoopEnd(oldX:Double) {
		if (loopSpan.isDefined) {
			val loopGot	= loopSpan.get
			val loopEnd	= loopGot.end
			if (oldX < loopEnd && x >= loopEnd) {
				fadeNowOrLater { 
					startFade(x - loopGot.size)
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------
	//## generator
	
	// depends on running, mode, pitch, rate
	private def updateV() {
		if (mode != Scratching) {
			v	= if (running) pitch * rate else 0
		}
	}
	
	// spring
	val dt	= 1.0 / outputRate	// delta time between steps
	val df	= 0.8				// damp factor
	val k	= 2000.0			// hardness
	val m	= 2.0				// mass
	var o	= 0.0				// origin, set by scratching

	var	xf	= 0.0	// position fading out
	var	x	= 0.0	// position fading in
	var v	= 0.0	// velocity
	var	a	= 0.0	// acceleration, non-zero when scratching
	
	private def afterEnd	= endFrame != Player.noEnd && x > endFrame
	
	def generate(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer) {
		// NOTE hack
		if (running && mode == Playing && afterEnd) {
			setRunning(false)
		}
		
		// current head
		val headSpeed	= abs(v)*dt
		val fadeinL		= Player.interpolation interpolate (inputL, x, headSpeed)
		val fadeinR		= Player.interpolation interpolate (inputR, x, headSpeed)
		
		// optionally fade out pre-jump head
		var audioL	= 0d
		var audioR	= 0d
		// stopFade sets fade to fadeMax
		if (isFading) {
			val fadeoutL	= Player.interpolation interpolate (inputL, xf, headSpeed)
			val fadeoutR	= Player.interpolation interpolate (inputR, xf, headSpeed)
			audioL	= fadeinL * fade + fadeoutL * (1-fade)
			audioR	= fadeinR * fade + fadeoutR * (1-fade)
			doFade()
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
		val filterValue	= filter.current
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
		if (v != zeroFrequency) {	// playing || scratchMode
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
			val	c	= df*2*sqrt(k*m)
			val d	= x-o	// -l
			a	= (-k*d - c*v)/m
			val v1	= v + a*dt
			v	= clampDouble(v1, -springSpeedLimit, springSpeedLimit)
		}
		
		val oldX	= x
		val move	= v*dt
		x	+= move
		xf	+= move
		
		if (mode == Playing) {
			jumpBackAfterLoopEnd(oldX)
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
