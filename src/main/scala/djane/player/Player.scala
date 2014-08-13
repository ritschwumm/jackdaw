package djane.player

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

import djane._
import djane.audio._
import djane.model._

object Player {
	val dampRate	= 0.1	// 0..1 range in 1/10 of a second
	val fadeTime	= 0.02	// 20 ms
	val endDelay	= 0.1	// 100 ms
	val noEnd		= -1d
	
	// filter modes
	private val filterLP	= -1
	private val filterOff	= 0
	private val filterHP	= +1
	
	// TODO check
	private val filterEpsilon	= 1.0E-5
}

/** 
one audio line outputting audio data for one Deck using a MixHalf
public methods must never be called outside the engine thread
*/
final class Player(metronome:Metronome, outputRate:Double, phoneEnabled:Boolean) {
	private val interpolation	= Config.sincEnabled cata (Linear, Sinc)
	
	private val equalizerL	= new Equalizer(Config.lowEq, Config.highEq, outputRate)
	private val equalizerR	= new Equalizer(Config.lowEq, Config.highEq, outputRate)
	
	private val filterL		= new BiQuad
	private val filterR		= new BiQuad
	
	private var sample:Sample			= EmptySample
	private var rhythm:Option[Rhythm]	= None
	private var rate:Double				= zeroFrequency
	private var inputL:Channel			= EmptyChannel
	private var inputR:Channel			= EmptyChannel
	
	// 0..1 range
	private val trim		= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	// -1..+1
	private val filter		= DamperDouble forRates (0.0,		Player.dampRate, outputRate)
	// 0..1 range
	private val low			= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	private val middle		= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	private val high		= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	// 0..1 range
	private val speaker		= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	private val phone		= DamperDouble forRates (unitGain,	Player.dampRate, outputRate)
	
	private val peakDetector	= new PeakDetector
	
	private var mode:PlayerMode		= PlayerMode.Playing
	private var scratchBase			= 0.0
	private val springSpeedLimit	= outputRate*100
	private var endFrame			= Player.noEnd
	
	private var running		= false
	private var pitch		= unitFrequency
	private var needSync	= true
	
	//------------------------------------------------------------------------------
	//## engine api
	
	private[player] def isRunning:Boolean	= running
	
	private[player]def feedback:PlayerFeedback	= 
			PlayerFeedback(
				running			= running,
				afterEnd		= afterEnd,
				position		= x,
				pitch			= pitch,
				measureMatch	= running flatGuard phaseMatch(RhythmUnit.Measure),
				beatRate		= beatRate,
				needSync		= needSync,
				hasSync			= hasSync,
				// NOTE this resets the peak detector
				masterPeak		= peakDetector.decay
			)
	
	private[player] def react(actions:ISeq[PlayerAction]) {
		actions foreach {
			case PlayerAction.RunningOn								=> runningOn()
			case PlayerAction.RunningOff							=> runningOff()
			case PlayerAction.PitchAbsolute(pitch)					=> pitchAbsolute(pitch)
			case PlayerAction.PhaseAbsolute(rhythmUnit, offset)		=> syncPhaseTo(rhythmUnit, offset)
			case PlayerAction.PhaseRelative(rhythmUnit, offset)		=> movePhaseBy(rhythmUnit, offset)
			case PlayerAction.PositionAbsolute(frame)				=> positionAbsolute(frame)
			case PlayerAction.PositionJump(frame, rhythmUnit)		=> positionJump(frame, rhythmUnit)
			case PlayerAction.PositionSeek(steps, rhythmUnit)		=> positionSeek(steps, rhythmUnit)
			case PlayerAction.DragBegin								=> dragBegin()
			case PlayerAction.DragEnd								=> dragEnd()
			case PlayerAction.DragAbsolute(v)						=> dragAbsolute(v)
			case PlayerAction.ScratchBegin							=> scratchBegin()
			case PlayerAction.ScratchEnd							=> scratchEnd()
			case PlayerAction.ScratchRelative(frames)				=> scratchRelative(frames)
			case PlayerAction.SetNeedSync(needSync)					=> setNeedSync(needSync)
			case c@PlayerAction.ChangeControl(_,_,_,_,_,_,_,_,_)	=> changeControl(c)
		}
	}
	
	//------------------------------------------------------------------------------
	//## common control
	
	private def changeControl(control:PlayerAction.ChangeControl) {
		trim	target	control.trim
		filter	target	control.filter
		low		target	control.low
		middle	target	control.middle
		high	target	control.high
		speaker	target	control.speaker
		phone	target	control.phone
		setSample(control.sample)
		setRhythm(control.rhythm)
	}

	private def setSample(sample1:Option[Sample]) {
		val sample	= sample1 getOrElse EmptySample
		if (sample == this.sample)	return
		this.sample	= sample
		
		inputL	= new SampleChannel(sample, 0)
		inputR	= new SampleChannel(sample, 1)
		rate	= sample.frameRate.toDouble
		
		endFrame	= sample.frameCount + outputRate*Player.endDelay
			
		updateV()
		keepSpeedSynced()
	}
	
	private def setRhythm(rhythm:Option[Rhythm]) {
		if (rhythm == this.rhythm)	return
		this.rhythm	= rhythm
		
		keepSpeedSynced()
	}
	
	private def setNeedSync(needSync:Boolean) {
		if (needSync == this.needSync)	return
		this.needSync	= needSync
		
		keepSpeedSynced()
	}
	
	//------------------------------------------------------------------------------
	//## beat rate
	
	/** called whenever the Metronome has changed its beatRate value */
	private [player] def metronomeBeatRateChanged() {
		keepSpeedSynced()
	}
	
	/** beats per second */
	private def beatRate:Option[Double]	=
			for {
				rhythm	<- rhythm
			}
			yield pitch * sample.frameRate / rhythm.beat
			
	//------------------------------------------------------------------------------
	//## sync
	
	private def hasSync:Boolean	=
			needSync && canSync
		
	private def canSync:Boolean	=
			sample != EmptySample &&
			rhythm.isDefined
	
	// depends on metronome.beatRate, beatRate (sample&rhythm) and needSync
	private def keepSpeedSynced() {
		if (!hasSync)	return
		beatRate foreach { beatRate =>
			pitch	= pitch * metronome.beatRate / beatRate
			updateV()
		}
	}
	
	/** set the phase to an absolute value measured in RasterUnits */
	private def syncPhaseTo(rhythmUnit:RhythmUnit, offset:Double) {
		for {
			phaseMatch	<- phaseMatch(rhythmUnit)
		} {
			movePhaseBy(rhythmUnit, offset - phaseMatch)
		}
	}
			
	/** change the phase by some offset measured in RasterUnits */
	private def movePhaseBy(rhythmUnit:RhythmUnit, offset:Double) {
		for {
			rhythm	<- rhythm
		} {
			startFade(x + offset * (rhythm raster rhythmUnit).size)
		}
	}
	
	/** how far we are from the rhythm of the Metronome in [-.5..+.5] rhythmUnits for late to early */
	private def phaseMatch(rhythmUnit:RhythmUnit):Option[Double]	=
			rhythm map { rhythm => 
				val here	= rhythm raster rhythmUnit phase x
				val there	= metronome	phase rhythmUnit
				moduloDouble(here - there + 0.5, 1) - 0.5
			}
			
	//------------------------------------------------------------------------------
	//## motor running
	
	private def runningOn() {
		changeRunning(true)
	}
	
	private def runningOff() {
		changeRunning(false)
	}
	
	private def changeRunning(running:Boolean) {
		this.running	= running
		updateV()
		
		// autosync on start
		if (needSync && canSync && running && mode == PlayerMode.Playing) {
			syncPhaseTo(RhythmUnit.Measure, 0)
		}
		
		stopFade()
	}
	
	//------------------------------------------------------------------------------
	//## motor position
	
	private def positionAbsolute(frame:Double) {
		startFade(frame)
	}
	
	/** jump to a given position without while staying in sync  */
	private def positionJump(frame:Double, rhythmUnit:RhythmUnit) {
		rhythm match {
			case Some(rhythm)	=>
				val	app		= rhythm raster rhythmUnit
				val raw		= (frame - x) / app.size
				val steps	= if (running) rint(raw) else raw
				positionSeek(steps, rhythmUnit)
			case None	=>
				positionAbsolute(frame)
		}
	}
	
	/** jump for a given number of rhythm while staying in sync */
	private def positionSeek(steps:Double, rhythmUnit:RhythmUnit) {
		// ignore small aberrations
		val	epsilon	= 1.0/10000
		
		// TODO stupid fake
		def fakeRhythm:Rhythm	= Rhythm simple (0, sample.frameRate)
				
		val	raster	= rhythm getOrElse fakeRhythm  raster rhythmUnit
		startFade(
				if (running) {
					 x + steps * raster.size
				}
				else {
					val	offset 	= (0.5 - epsilon) * signum(steps)
					val	raw		= x + (steps - offset) * raster.size
					raster round raw
				})
	}
					
	//------------------------------------------------------------------------------
	//## motor speed
	
	private def pitchAbsolute(pitch:Double)  {
		if (this.pitch == pitch)	return	
		this.pitch	= pitch
		updateV()
		
		needSync	= false
	}
			
	//------------------------------------------------------------------------------
	//## motor scratch
	
	// motor -> scratch
	private def scratchBegin() {
		mode		= PlayerMode.Scratching
		scratchBase	= x
		o			= x
		a			= 0
		v			= 0
	}
	
	private def scratchRelative(frames:Double) {
		if (mode != PlayerMode.Scratching)	throw new IllegalArgumentException("not in scratch mode??")
		o	= scratchBase - frames
	}
	
	// scratch -> motor
	private def scratchEnd() {
		mode	= PlayerMode.Playing
		updateV()
	}
	
	//------------------------------------------------------------------------------
	//## motor drag
	
	// motor -> scratch
	private def dragBegin() {
		mode	= PlayerMode.Dragging
	}
	
	private def dragAbsolute(speed:Double) {
		if (mode != PlayerMode.Dragging)	throw new IllegalArgumentException("not in drag mode??")
		v	= speed * rate
	}
	
	// scratch -> motor
	private def dragEnd() {
		mode	= PlayerMode.Playing
		updateV()
	}
	
	//------------------------------------------------------------------------------
	//## generator
	
	// depends on running, mode, pitch, rate
	private def updateV() {
		if (mode != PlayerMode.Scratching) {
			v	= if (running) pitch * rate else 0
		}
	}
	
	private def startFade(newX:Double) {
		xf		= x
		x		= newX
		fade	= fadeMin
	}
	
	private def stopFade() {
		fade	= fadeMax
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
	private def autoStop	= afterEnd && running && mode == PlayerMode.Playing
	
	private val fadeStep	= 1.0 / (Player.fadeTime * outputRate)
	private val fadeMin		= 0.0
	private val fadeMax		= 1.0
	private var fade		= fadeMax
	
	private var filterModeOld	= Player.filterOff
		
	def generate(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer) {
		// NOTE hack
		if (autoStop) {
			runningOff()
		}
		
		// current head
		val headSpeed	= abs(v)*dt
		val fadeinL		= interpolation interpolate (inputL, x, headSpeed)
		val fadeinR		= interpolation interpolate (inputR, x, headSpeed)
		
		// optionally fade out pre-jump head
		var audioL	= 0d
		var audioR	= 0d
		if (fade != fadeMax) {
			val fadeoutL	= interpolation interpolate (inputL, xf, headSpeed)
			val fadeoutR	= interpolation interpolate (inputR, xf, headSpeed)
			audioL	= fadeinL * fade + fadeoutL * (1-fade)
			audioR	= fadeinR * fade + fadeoutR * (1-fade)
			fade	+= fadeStep
			if (fade >= fadeMax) {
				stopFade()
			}
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
		
		if (mode == PlayerMode.Scratching) {
			val	c	= df*2*sqrt(k*m)
			val d	= x-o	// -l
			a	= (-k*d - c*v)/m
			val v1	= v + a*dt
			v	= clampDouble(v1, -springSpeedLimit, springSpeedLimit)
		}
		
		val move	= v*dt
		x	+= move
		xf	+= move
		
		trim.step()
		filter.step()
		low.step()
		middle.step()
		high.step()
		speaker.step()
		phone.step()
	}
	
	// (0+filterLow)..(nyquist-gilterHigh), predivided by outputrate
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
}
