package jackdaw.player

import java.io.File

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.time._

import scaudio.sample.Sample
import scaudio.interpolation.Sinc

import jackdaw.Config
import jackdaw.concurrent._

object Loader {
	private val actorPriority:Int			= (Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2
	private val cycleDelay:MilliDuration	= 10.millis
}

final class Loader(outputRate:Double, engineExecute:Effect[Task]) {
	private val actor	=
			Actor[LoaderAction](
				"sample preloader",
				Loader.actorPriority,
				Loader.cycleDelay,
				message	=> {
					reactAction(message)
					true
				}
			)
	
	def start() {
		actor.start()
	}
	
	def dispose() {
		actor.dispose()
	}
	
	def enqueueAction(action:LoaderAction) {
		actor send action
	}
	
	//------------------------------------------------------------------------------
	
	private val bufferFrames:Int	= ceil(Config.preloadTime.millis * outputRate / 1000 + Player.maxDistance).toInt
	
	private val reactAction:Effect[LoaderAction]	=
			_ match {
				case LoaderPreload(sample, frame)	=> preload(sample, frame)
				case LoaderNotifyEngine(task)		=> engineExecute(task)
			}
	
	private def preload(sample:Sample, frame:Int) {
		// Sample.empty has zero
		if (sample.frameBytes != 0) {
			val blockFrames:Int	= Config.diskBlockSize / sample.frameBytes
			val first	= frame - bufferFrames
			val last	= frame + bufferFrames
			var curr	= first
			while (curr <= last) {
				val count	= sample.channels.size
				var index	= 0
				while (index < count) {
					sample.channels apply index get curr
					index	+= 1
				}
				curr	+= blockFrames
			}
		}
	}
}
