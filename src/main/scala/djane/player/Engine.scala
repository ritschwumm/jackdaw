package djane.player

import scala.math._

import scutil.lang._
import scutil.log._

import scaudio.control._
import scaudio.output._
import scaudio.math._

import djane._

object Engine {
	private val dampRate	= 0.1 
}

/** generates audio data using audio Player objects and a Metronome */
final class Engine extends Logging {
	private val	output		= new Output(Config.outputConfig, producer)
	private val outputInfo	= output.info
	val phoneEnabled	= outputInfo.headphone
	DEBUG("phone enabled", phoneEnabled)
	
	private val speaker	= DamperDouble forRates	(unitGain, Engine.dampRate, outputInfo.rate)
	private val phone	= DamperDouble forRates	(unitGain, Engine.dampRate, outputInfo.rate)
	
	private val metronome	= new Metronome(outputInfo.rate, new MetronomeContext {
		def beatRateChanged(beatRate:Double) { changeBeatRate() }
		def running:Boolean	= playerRunning
	})
	
	private val peakDetector	= new PeakDetector
	
	private val player1	= new Player(
			metronome, 
			outputInfo.rate, 
			outputInfo.headphone)
	private val player2	= new Player(
			metronome, 
			outputInfo.rate,
			outputInfo.headphone)
	private val player3	= new Player(
			metronome, 
			outputInfo.rate,
			outputInfo.headphone)
	
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
		output.start()
	}
	
	def dispose() {
		output.dispose()
	}
	
	// output rate adapted to nanoTime jitter
	private var feedbackRate	= outputInfo.rate.toDouble
	 
	def feedbackTimed(deltaNanos:Long):Option[EngineFeedback]	= {
		val frames	= deltaNanos.toDouble * feedbackRate / 1000000000D
		val blocks	= round(frames / Config.controlFrames).toInt
		
		// BETTER exponential smoothing of diff
		
		// smooth nanoTime jitter
		val overshot	= outgoing.size - blocks
		val diff		= overshot - Config.queueOvershot
		val shaped		= tanh(diff.toDouble / Config.queueOvershot)
		val factor		= pow(Config.rateFactor, shaped)
		feedbackRate	= feedbackRate * factor
		
		var block	= 0
		var old		= None:Option[EngineFeedback]
		while (block < blocks) {
			val cur	= outgoing.receive
			if (cur == None) {
				return old
			}
			old		= cur
			block	+= 1
		}
		old
	}
	
	def react(actions:ISeq[EngineAction]) {
		incoming send actions
	}
	
	// TODO lock ugly
	
	def reactPlayer(player:Int)(actions:ISeq[PlayerAction]) {
		react(Vector(EngineAction.ControlPlayer(player, actions)))
	}
	
	//------------------------------------------------------------------------------
	//## communication
	
	private var frame:Long	= 0
	
	private val incoming	= new TransferQueue[ISeq[EngineAction]]
	private val outgoing	= new TransferQueue[EngineFeedback]
	
	private def receiveControl() {
		incoming.receive foreach { actions =>
			actions foreach {
				case EngineAction.SetBeatRate(beatRate)		=> metronome setBeatRate beatRate
				case c@EngineAction.ChangeControl(_, _)		=> changeControl(c)
				case EngineAction.ControlPlayer(1,actions)	=> player1 react actions
				case EngineAction.ControlPlayer(2,actions)	=> player2 react actions
				case EngineAction.ControlPlayer(3,actions)	=> player3 react actions
				case EngineAction.ControlPlayer(x,_)			=> ERROR("unexpected player", x)
			}
			// loop until the queue is empty
			receiveControl()
		}
	}
	
	private def changeControl(control:EngineAction.ChangeControl) {
		speaker	target	control.speaker
		phone	target	control.phone
	}
	
	private def sendFeedback() {
		outgoing send mkFeedback
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
	
	private object producer extends FrameProducer {
		def produce(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer) {
			val communicate	= frame % Config.controlFrames == 0
			
			if (communicate) {
				receiveControl()
			}
			
			player1 generate (speakerBuffer,	phoneBuffer)
			player2 generate (speakerBuffer,	phoneBuffer)
			player3 generate (speakerBuffer,	phoneBuffer)
			
			val speakerScale	= speaker.current.toFloat
			speakerBuffer mul (speakerScale, speakerScale)
			
			if (outputInfo.headphone) {
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
