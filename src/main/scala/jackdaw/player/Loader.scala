package jackdaw.player

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.time._

import scaudio.sample.Sample
import scaudio.interpolation.Sinc

import jackdaw.Config
import jackdaw.util.TransferQueue

object Loader {
	// TODO loader stupid
	private val cycleDelay:MilliDuration	= 5.millis
}

final class Loader(outputRate:Double, engineExecute:Effect[Task]) {
	private val incomingActions	= new TransferQueue[LoaderAction]
	
	private object thread extends Thread {
		@volatile 
		var keepAlive	= true
		
		setName("sample preloader")
		setPriority((Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2)
		
		override def run() {
			while (keepAlive) {
				receiveActions()
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
	
	def enqueueAction(action:LoaderAction) {
		incomingActions send action
		// TODO loader broken
		// incoming.notify
	}
	
	//------------------------------------------------------------------------------
	
	private val bufferFrames:Int	= ceil(Config.preloadTime.millis * outputRate / 1000 + Player.maxDistance).toInt
	
	private def receiveActions() {
		incomingActions receiveWith reactAction
		// TODO loader broken
		// incoming wait cycleDelay
		Thread sleep Loader.cycleDelay.millis
	}
	
	private def reactAction(message:LoaderAction) {
		message match {
			case LoaderPreload(sample, frame)	=> preload(sample, frame)
			case LoaderNotifyEngine(task)		=> engineExecute(task)
		}
	}
	
	private def preload(sample:Sample, frame:Int) {
		// Sample.empty has zero
		if (sample.frameBytes != 0) {
			val blockFrames:Int	= Config.diskBlockSize / sample.frameBytes
			(frame - bufferFrames) to (frame + bufferFrames) by blockFrames foreach { load	=>
				sample.channels foreach { channel =>
					channel get load 
				}
			}
		}
	}
}
