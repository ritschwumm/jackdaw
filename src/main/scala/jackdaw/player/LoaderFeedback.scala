package jackdaw.player

import scutil.lang._

sealed trait LoaderFeedback

final case class LoaderExecute(
	task:Thunk[Unit]
)
extends LoaderFeedback
