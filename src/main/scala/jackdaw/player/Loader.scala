package jackdaw.player

import java.io.File

import scutil.core.implicits._
import scutil.io.implicits._
import scutil.lang._
import scutil.time._
import scutil.log._

import scaudio.sample._

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
	val target:Target[LoaderAction]	= actor.asTarget

	def start():Unit	= {
		actor.start()
	}

	def close():Unit	= {
		actor.close()
	}

	//------------------------------------------------------------------------------

	private val reactAction:Effect[LoaderAction]	=
		_ match {
			// BETTER don't close over the player, we already know it
			case LoaderAction.Decode(file, callback)		=> doDecode(file, callback)
			case LoaderAction.Preload(sample, centerFrame)	=> doPreload(sample, centerFrame)
			case LoaderAction.NotifyEngine(task)			=> doInEngine(task)
		}

	private def doDecode(file:File, callback:Effect[Option[CacheSample]]):Unit	= {
		DEBUG("loader loading", file)
		val sample	=
			(Wav load file)
			.leftEffect	{ it => ERROR("cannot load file", it) }
			.toOption
			.map { new CacheSample(_) }
		doInEngine(thunk {
			callback(sample)
		})
	}

	private def doPreload(sample:CacheSample, centerFrame:Int):Unit	= {
		val changed		= sample provide centerFrame
		if (changed) {
			sample.writeBarrier()
			// BETTER don't close over the sample, we already know it
			doInEngine(thunk {
				sample.readBarrier()
			})
		}
	}

	private def doInEngine(task:Thunk[Unit]):Unit	= {
		 engineTarget send LoaderExecute(task)
	}
}
