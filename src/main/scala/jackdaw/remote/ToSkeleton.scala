package jackdaw.remote

import jackdaw.player.*

object ToSkeleton {
	case object Kill							extends ToSkeleton
	final case class Send(action:EngineAction)	extends ToSkeleton
}

sealed trait ToSkeleton
