package jackdaw.remote

import jackdaw.player._

sealed trait ToSkeleton

case object KillSkeleton						extends ToSkeleton
case class SendSkeleton(action:EngineAction)	extends ToSkeleton
