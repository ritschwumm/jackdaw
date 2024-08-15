package jackdaw.player

import scutil.lang.*
import scutil.log.*

import scaudio.control.*
import scaudio.output.*
import scaudio.math.*

import jackdaw.*
import jackdaw.concurrent.*

/** generates audio data using audio Player objects and a Metronome */
object Engine {
	// 0..1 range in 1/10 of a second
	private val dampTime	= 0.1

	// TODO using should return an Io
	def create(output:Output, feedbackTarget:Target[EngineFeedback]):IoResource[EngineAction=>Unit]	=
		for {
			incomingActionQueue		<-	IoResource.delay { new Transfer[EngineAction] }
			loaderFeedbackQueue		<-	IoResource.delay { new Transfer[LoaderFeedback] }
			loaderTarget1			<-	Loader.create(loaderFeedbackQueue.asTarget)
			loaderTarget2			<-	Loader.create(loaderFeedbackQueue.asTarget)
			loaderTarget3			<-	Loader.create(loaderFeedbackQueue.asTarget)
			producer				=	new Engine(
											incomingActionQueue,
											loaderFeedbackQueue,
											feedbackTarget,
											loaderTarget1,
											loaderTarget2,
											loaderTarget3,
											output.outputInfo.rate,
											output.outputInfo.headphone
										)
			output					<-	output.runProducerUsingConsumer(producer)
		}
		yield {
			incomingActionQueue.send(_)
		}
}

private final class Engine(
	incomingActionQueue:Transfer[EngineAction],
	loaderFeedbackQueue:Transfer[LoaderFeedback],
	feedbackTarget:Target[EngineFeedback],
	loaderTarget1:Target[LoaderAction],
	loaderTarget2:Target[LoaderAction],
	loaderTarget3:Target[LoaderAction],
	outputRate:Int,
	phoneEnabled:Boolean
)
extends FrameProducer with Logging {
	private var frame:Long	= 0

	private val peakDetector	= new PeakDetector

	private val metronome	= new Metronome(outputRate)
	private val player1		= new Player(metronome, outputRate, phoneEnabled, loaderTarget1)
	private val player2		= new Player(metronome, outputRate, phoneEnabled, loaderTarget2)
	private val player3		= new Player(metronome, outputRate, phoneEnabled, loaderTarget3)
	private val speaker		= DamperDouble.forRates(unitGain, Engine.dampTime, outputRate)
	private val phone		= DamperDouble.forRates(unitGain, Engine.dampTime, outputRate)

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

		peakDetector.put(speakerBuffer.left)
		peakDetector.put(speakerBuffer.right)

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

	//------------------------------------------------------------------------------
	//## incoming loader communication

	private def receiveLoading():Unit	= {
		loaderFeedbackQueue.receiveAll(reactLoading)
	}

	private val reactLoading:Effect[LoaderFeedback]	=
		_ match {
			case LoaderFeedback.Execute(task)	=> task()
		}

	//------------------------------------------------------------------------------
	//## incoming model communication

	private def receiveActions():Unit	= {
		incomingActionQueue.receiveAll(reactAction)
	}

	private val reactAction:Effect[EngineAction]	=
		_ match {
			case EngineAction.ChangeControl(speakerValue, phoneValue)	=>
				speaker	.target(speakerValue)
				phone	.target(phoneValue)
			case EngineAction.SetBeatRate(beatRate)		=>
				metronome.setBeatRate(beatRate)
				player1.metronomeBeatRateChanged()
				player2.metronomeBeatRateChanged()
				player3.metronomeBeatRateChanged()
			case EngineAction.ControlPlayer(1, action)	=> player1.react(action)
			case EngineAction.ControlPlayer(2, action)	=> player2.react(action)
			case EngineAction.ControlPlayer(3, action)	=> player3.react(action)
			case EngineAction.ControlPlayer(x, _)		=> ERROR("unexpected player", x)
		}

	//------------------------------------------------------------------------------
	//## outgoing model communication

	private def sendFeedback():Unit	= {
		feedbackTarget.send(mkFeedback)
	}

	// NOTE this resets the peak detectors
	private def mkFeedback:EngineFeedback	=
		EngineFeedback(
			masterPeak	= peakDetector.decay,
			player1		= player1.feedback,
			player2		= player2.feedback,
			player3		= player3.feedback
		)
}
