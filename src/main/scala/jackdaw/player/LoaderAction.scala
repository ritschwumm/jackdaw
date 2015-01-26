package jackdaw.player

import scutil.lang.Task

import scaudio.sample.Sample

sealed trait LoaderAction

case class LoaderPreload(
	sample:Sample,
	centerFrame:Int,
	bufferFrames:Int
)
extends LoaderAction

case class LoaderNotifyEngine(
	done:Task
)
extends LoaderAction
