package jackdaw.player

import scutil.lang._

sealed trait LoaderFeedback

case class LoaderExecute(
	task:Task
)
extends LoaderFeedback
