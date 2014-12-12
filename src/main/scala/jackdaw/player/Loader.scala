package jackdaw.player

import scala.math._

import scutil.lang._

import scaudio.sample.Sample
import scaudio.interpolation.Sinc

final class Loader(outputRate:Double, engineExecute:Effect[Task]) {
	// TODO hardcoded
	private val blockBytes		= 4096
	private val bufferSeconds	= 1	// TODO should depend on BPM and loop size
	private val cycleDelay		= 5	// millis
	
	//------------------------------------------------------------------------------
	
	private val incoming	= new TransferQueue[LoaderAction]
	
	private object thread extends Thread {
		@volatile 
		var keepAlive	= true
		
		setName("Loader")
		setPriority((Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2)
		
		override def run() {
			while (keepAlive) {
				receiveAndReact()
			}
		}
	}
	
	def start() {
		thread.start()
	}
	
	def dispose() {
		thread.keepAlive	= false
		thread.join()
	}
	
	def handle(action:LoaderAction) {
		incoming send action
		// TODO loader broken
		// incoming.notify
	}
	
	//------------------------------------------------------------------------------
	
	private val bufferFrames:Int	= ceil(bufferSeconds * outputRate + Player.maxDistance).toInt
	
	private def receiveAndReact() {
		incoming.receiveWith {
			case LoaderAction.Preload(sample, frame)	=>
				preload(sample, frame)
			case LoaderAction.NotifyEngine(task)	=>
				engineExecute(task)
		}
		// TODO loader broken
		// incoming wait cycleDelay
		Thread sleep cycleDelay
	}
	
	private def preload(sample:Sample, frame:Int) {
		// Sample.empty has zero
		if (sample.frameBytes != 0) {
			val blockFrames:Int		= blockBytes / sample.frameBytes
			frame - bufferFrames to frame + bufferFrames by blockFrames foreach { frame =>
				sample.channels foreach { channel =>
					channel get frame 
				}
			}
		}
	}
}
