package jackdaw.player

import scutil.lang.implicits._
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
final class Engine(feedbackTarget:Target[EngineFeedback]) extends Logging {
	private val	output		= new Output(Config.outputConfig, producer)
	private val outputInfo	= output.info

	val outputRate		= outputInfo.rate
	val phoneEnabled	= outputInfo.headphone

	private val incomingActionQueue		= new Transfer[EngineAction]
	private val loaderFeedbackQueue		= new Transfer[LoaderFeedback]

	private val loader1	= new Loader(loaderFeedbackQueue.asTarget)
	private val loader2	= new Loader(loaderFeedbackQueue.asTarget)
	private val loader3	= new Loader(loaderFeedbackQueue.asTarget)

	private val metronome	= new Metronome(outputRate)

	private val speaker	= DamperDouble.forRates(unitGain, Engine.dampTime, outputRate)
	private val phone	= DamperDouble.forRates(unitGain, Engine.dampTime, outputRate)

	private val peakDetector	= new PeakDetector

	private val player1	=
		new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader1.target
		)
	private val player2	=
		new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader2.target
		)
	private val player3	=
		new Player(
			metronome,
			outputRate,
			phoneEnabled,
			loader3.target
		)

	//------------------------------------------------------------------------------
	//## public api

	def start():Unit	= {
		loader1.start()
		loader2.start()
		loader3.start()
		output.start()
	}

	def close():Unit	= {
		output.close()
		loader1.close()
		loader2.close()
		loader3.close()
	}

	//------------------------------------------------------------------------------
	//## incoming loader communication

	private def receiveLoading():Unit	= {
		loaderFeedbackQueue receiveAll reactLoading
	}

	private val reactLoading:Effect[LoaderFeedback]	=
		_ match {
			case LoaderExecute(task)	=> task()
		}

	//------------------------------------------------------------------------------
	//## incoming model communication

	def enqueueAction(action:EngineAction):Unit	= {
		incomingActionQueue send action
	}

	private def receiveActions():Unit	= {
		incomingActionQueue receiveAll reactAction
	}

	private val reactAction:Effect[EngineAction]	=
		_ match {
			case EngineAction.ChangeControl(speakerValue, phoneValue)	=>
				speaker	target	speakerValue
				phone	target	phoneValue
			case EngineAction.SetBeatRate(beatRate)		=>
					metronome setBeatRate beatRate
					player1.metronomeBeatRateChanged()
					player2.metronomeBeatRateChanged()
					player3.metronomeBeatRateChanged()
			case EngineAction.ControlPlayer(1, action)	=> player1 react action
			case EngineAction.ControlPlayer(2, action)	=> player2 react action
			case EngineAction.ControlPlayer(3, action)	=> player3 react action
			case EngineAction.ControlPlayer(x, _)		=> ERROR("unexpected player", x)
		}

	//------------------------------------------------------------------------------
	//## outgoing model communication

	private def sendFeedback():Unit	= {
		feedbackTarget send mkFeedback
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
		def produce(speakerBuffer:FrameBuffer, phoneBuffer:FrameBuffer):Unit	= {
			val talkToGui	= frame % Config.guiIntervalFrames == 0
			if (talkToGui) {
				receiveActions()
			}
			val talkToLoader	= frame % Config.loaderIntervalFrames == 0
			if (talkToLoader) {
				// this indirectly executed loader answer thunks registered by the player
				receiveLoading()
				player1.talkToLoader()
				player2.talkToLoader()
				player3.talkToLoader()
			}

			player1.generate(speakerBuffer,	phoneBuffer)
			player2.generate(speakerBuffer,	phoneBuffer)
			player3.generate(speakerBuffer,	phoneBuffer)

			val speakerScale	= speaker.current.toFloat
			speakerBuffer.mul(speakerScale, speakerScale)

			if (phoneEnabled) {
				val phoneScale	= phone.current.toFloat
				phoneBuffer.mul(phoneScale, phoneScale)
			}

			peakDetector put speakerBuffer.left
			peakDetector put speakerBuffer.right

			metronome.step(
				player1.isRunning	||
				player2.isRunning	||
				player3.isRunning
			)
			speaker.step()
			phone.step()

			if (talkToGui) {
				sendFeedback()
				// Thread.`yield`()
			}

			frame	= frame + 1
		}
	}
}
