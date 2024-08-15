package jackdaw.remote

import jackdaw.player.*

enum ToSkeleton {
	case Kill
	case Send(action:EngineAction)
}
