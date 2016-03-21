package jackdaw.player

import scutil.lang._

sealed trait LoaderFeedback

final case class LoaderExecute(
	task:Task
)
extends LoaderFeedback
