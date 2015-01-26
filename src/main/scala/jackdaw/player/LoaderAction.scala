package jackdaw.player

import java.io.File

import scutil.lang._

import scutil.lang._

sealed trait LoaderAction

case class LoaderDecode(
	file:File,
	done:Effect[Option[CacheSample]]
)
extends LoaderAction

case class LoaderPreload(
	sample:CacheSample,
	centerFrame:Int
)
extends LoaderAction

case class LoaderNotifyEngine(
	done:Task
)
extends LoaderAction
