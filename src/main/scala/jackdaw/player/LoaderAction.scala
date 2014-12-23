package jackdaw.player

import scutil.lang.Task

import scaudio.sample.Sample

sealed trait LoaderAction

case class LoaderPreload(sample:Sample, frame:Int)	extends LoaderAction
case class LoaderNotifyEngine(done:Task) 			extends LoaderAction
