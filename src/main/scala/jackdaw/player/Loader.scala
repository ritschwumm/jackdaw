package jackdaw.player

import java.io.File

import scutil.lang._
import scutil.implicits._
import scutil.time._
import scutil.log._

import scaudio.sample._
import scaudio.interpolation.Sinc

import jackdaw.concurrent._

object Loader {
	private val actorPriority:Int			= (Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2
	private val cycleDelay:MilliDuration	= 10.millis
}

final class Loader(engineTarget:Target[LoaderFeedback]) extends Logging {
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
				// BETTER don't close over the player, we already know it
				case LoaderDecode(file, callback)		=> decode(file, callback)
				case LoaderPreload(sample, centerFrame)	=> preload(sample, centerFrame)
				case LoaderNotifyEngine(task)			=> doInEngine(task)
			}
	
	private def decode(file:File, callback:Effect[Option[CacheSample]]) {
		DEBUG("loader loading", file)
		val sample	=
				(Wav load file)
				.failEffect	{ it => ERROR("cannot load file", it) }
				.toOption
				.map { new CacheSample(_) }
		doInEngine(thunk {
			callback(sample)
		})
	}
	
	private def preload(sample:CacheSample, centerFrame:Int) {
		val changed		= sample provide centerFrame
		if (changed) {
			sample.writeBarrier()
			// BETTER don't close over the sample, we already know it
			doInEngine(thunk {
				sample.readBarrier()
			})
		}
	}
	
	// TODO cleanup naming
	private def doInEngine(task:Task) {
		 engineTarget send LoaderExecute(task)
	}
}
