package jackdaw.player

import java.io.File

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

final class Loader(engineTarget:Target[LoaderFeedback]) {
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
	val target	= actor.asTarget
	
	def start() {
		actor.start()
	}
	
	def dispose() {
		actor.dispose()
	}
	
	//------------------------------------------------------------------------------
	
	private val reactAction:Effect[LoaderAction]	=
			_ match {
				case LoaderPreload(sample, frame, bufferFrames)	=> preload(sample, frame, bufferFrames)
				case LoaderNotifyEngine(task)					=> doInEngine(task)
			}
	
	private def preload(sample:Sample, centerFrame:Int, bufferFrames:Int) {
		// Sample.empty has zero
		if (sample.frameBytes != 0) {
			val blockFrames:Int	= Config.preloadDiskBlockSize / sample.frameBytes
			val first	= centerFrame - bufferFrames
			val last	= centerFrame + bufferFrames
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
	
	// TODO cleanup naming
	private def doInEngine(task:Task) {
		 engineTarget send LoaderExecute(task)
	}
}
