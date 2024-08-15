package jackdaw.player

import scutil.lang.*

enum LoaderFeedback {
	case Execute(task:Thunk[Unit])
}
