package jackdaw.player

import scala.math._

import scutil.lang._
import scutil.log._

import scaudio.control._
import scaudio.output._
import scaudio.math._

import jackdaw._
import jackdaw.concurrent._

object Engine {
	// 0..1 range in 1/10 of a second
	private val dampTime	= 0.1
}

/** generates audio data using audio Player objects and a Metronome */
final class Engine extends Logging {
	private val	output		= new Output(Config.outputConfig, producer)
	private val outputInfo	= output.info
	
	val outputRate		= outputInfo.rate
	val phoneEnabled	= outputInfo.headphone
	DEBUG("output rate", outputRate)
	DEBUG("phone enabled", phoneEnabled)
	
	private val speaker	= DamperDouble forRates	(unitGain, Engine.dampTime, outputRate)
	private val phone	= DamperDouble forRates	(unitGain, Engine.dampTime, outputRate)
	
	private val loader	=
			new Loader(outputRate, enqueueLoading _)
	
	private val metronome	=
			new Metronome(outputRate, new MetronomeContext {
				// TODO is this good for anything?
				// it just forwards the argument to setBeatRate
				def beatRateChanged(beatRate:Double) { changeBeatRate() }
				def running:Boolean	= playerRunning
			})
			
	private val peakDetector	= new PeakDetector
	
	private val player1	= new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader.enqueueAction _)
	private val player2	= new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader.enqueueAction _)
	private val player3	= new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader.enqueueAction _)
	
	private def playerRunning:Boolean	=
			player1.isRunning	||
			player2.isRunning	||
			player3.isRunning
	
	private def changeBeatRate() {
		player1.metronomeBeatRateChanged()
		player2.metronomeBeatRateChanged()
		player3.metronomeBeatRateChanged()
	}
	
	//------------------------------------------------------------------------------
	//## public api
	
	def start() {
		loader.start()
		output.start()
	}
	
	def dispose() {
		output.dispose()
		loader.dispose()
	}
	
	//------------------------------------------------------------------------------
	//## incoming loader communication
	
	private val loaderFeedbackQueue		= new Transfer[Task]
	
	private def enqueueLoading(task:Task) {
		loaderFeedbackQueue send task
	}
	
	private def receiveLoading() {
		loaderFeedbackQueue receiveAll reactLoading
	}
	
	private val reactLoading:Effect[Task]	=
			_ apply ()
	
	//------------------------------------------------------------------------------
	//## incoming model communication
	
	private val incomingActionQueue		= new Transfer[EngineAction]
	
	def enqueueAction(action:EngineAction) {
		incomingActionQueue send action
	}
	
	private def receiveActions() {
		incomingActionQueue receiveAll reactAction
	}
	
	private val reactAction:Effect[EngineAction]	=
			_ match {
				case c@EngineChangeControl(_,_)			=> changeControl(c)
				case EngineSetBeatRate(beatRate)		=> metronome setBeatRate beatRate
				case EngineControlPlayer(1, action)		=> player1 react action
				case EngineControlPlayer(2, action)		=> player2 react action
				case EngineControlPlayer(3, action)		=> player3 react action
				case EngineControlPlayer(x, _)			=> ERROR("unexpected player", x)
			}
	
	private def changeControl(control:EngineChangeControl) {
		speaker	target	control.speaker
		phone	target	control.phone
	}
	
	//------------------------------------------------------------------------------
	//## outgoing model communication
	
	private val outgoingFeedbackQueue	= new Transfer[EngineFeedback]
	
	// output rate adapted to nanoTime jitter
	private var feedbackRate	= outputRate.toDouble
	 
	def feedbackTimed(deltaNanos:Long):Option[EngineFeedback]	= {
		val frames	= deltaNanos.toDouble * feedbackRate / 1000000000D
		val blocks	= round(frames / Config.controlFrames).toInt
		
		// BETTER exponential smoothing of diff
		
		// smooth nanoTime jitter
		val overshot	= outgoingFeedbackQueue.available - blocks
		val diff		= overshot - Config.queueOvershot
		val shaped		= tanh(diff.toDouble / Config.queueOvershot)
		val factor		= pow(Config.rateFactor, shaped)
		feedbackRate	= feedbackRate * factor
		
		var block	= 0
		var old		= None:Option[EngineFeedback]
		while (block < blocks) {
			val cur	= outgoingFeedbackQueue.receive
			if (cur == None) {
				return old
			}
			old		= cur
			block	+= 1
		}
		old
	}
	
	private def sendFeedback() {
		outgoingFeedbackQueue send mkFeedback
	}
	
	// NOTE this resets the peak detectors
	private def mkFeedback:EngineFeedback	= 
			EngineFeedback(
				masterPeak	= peakDetector.decay,
				player1		= player1.feedback,
				player2		= player2.feedback,
				player3		= player3.feedback
			)
	
	//------------------------------------------------------------------------------
	
	private var frame:Long	= 0
	
	private object producer extends FrameProducer {
		def produce(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer) {
			val communicate	= frame % Config.controlFrames == 0
			
			if (communicate) {
				receiveActions()
				receiveLoading()
				player1.preloadCurrent()
				player2.preloadCurrent()
				player3.preloadCurrent()
			}
			
			player1 generate (speakerBuffer,	phoneBuffer)
			player2 generate (speakerBuffer,	phoneBuffer)
			player3 generate (speakerBuffer,	phoneBuffer)
			
			val speakerScale	= speaker.current.toFloat
			speakerBuffer mul (speakerScale, speakerScale)
			
			if (phoneEnabled) {
				val phoneScale	= phone.current.toFloat
				phoneBuffer	mul (phoneScale, phoneScale)
			}
			
			peakDetector put speakerBuffer.left
			peakDetector put speakerBuffer.right
			
			metronome.step()
			speaker.step()
			phone.step()
			
			if (communicate) {
				sendFeedback()
				// Thread.`yield`()
			}
			
			frame	= frame + 1
		}
	}
}
