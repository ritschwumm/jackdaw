package jackdaw.player

import scutil.lang.*

sealed trait LoaderFeedback

final case class LoaderExecute(
	task:Thunk[Unit]
)
extends LoaderFeedback
