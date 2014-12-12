package jackdaw.player

import scutil.lang.Task

import scaudio.sample.Sample

object LoaderAction {
	case class Preload(sample:Sample, frame:Int)	extends LoaderAction
	case class NotifyEngine(done:Task) 				extends LoaderAction
}

sealed trait LoaderAction
