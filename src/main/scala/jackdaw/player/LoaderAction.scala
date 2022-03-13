package jackdaw.player

import java.io.File

import scutil.lang.*

object LoaderAction {
	final case class Decode(
		file:File,
		done:Effect[Option[CacheSample]]
	)
	extends LoaderAction

	final case class Preload(
		sample:CacheSample,
		centerFrame:Int
	)
	extends LoaderAction

	final case class NotifyEngine(
		done:Thunk[Unit]
	)
	extends LoaderAction
}

sealed trait LoaderAction
