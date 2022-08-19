package jackdaw.player

import java.nio.file.Path

import scutil.core.implicits.*
import scutil.io.implicits.*
import scutil.lang.*
import scutil.time.*
import scutil.log.*

import scaudio.sample.*

import jackdaw.concurrent.*

object Loader extends Logging {
	private val actorPriority:Int			= (Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2
	private val cycleDelay:MilliDuration	= 10.millis

	def create(engineTarget:Target[LoaderFeedback]):IoResource[Target[LoaderAction]]	= {
		val loader	= new Loader(engineTarget)
		Actor.create[LoaderAction](
			"sample preloader",
			Loader.actorPriority,
			Loader.cycleDelay,
			message	=> {
				loader.reactAction(message)
				true
			}
		)
	}
}

final class Loader(engineTarget:Target[LoaderFeedback]) extends Logging {
	val reactAction:Effect[LoaderAction]	=
		_ match {
			// BETTER don't close over the player, we already know it
			case LoaderAction.Decode(file, callback)		=> doDecode(file, callback)
			case LoaderAction.Preload(sample, centerFrame)	=> doPreload(sample, centerFrame)
			case LoaderAction.NotifyEngine(task)			=> doInEngine(task)
		}

	private def doDecode(file:Path, callback:Effect[Option[CacheSample]]):Unit	= {
		DEBUG("loader loading", file)
		val sample	=
			Wav.load(file)
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
