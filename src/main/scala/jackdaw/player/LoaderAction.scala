package jackdaw.player

import java.io.File

import scutil.lang._

import scutil.lang._

sealed trait LoaderAction

final case class LoaderDecode(
	file:File,
	done:Effect[Option[CacheSample]]
)
extends LoaderAction

final case class LoaderPreload(
	sample:CacheSample,
	centerFrame:Int
)
extends LoaderAction

final case class LoaderNotifyEngine(
	done:Task
)
extends LoaderAction
