package jackdaw.remote

import jackdaw.player._

sealed trait ToSkeleton

case object KillSkeleton							extends ToSkeleton
final case class SendSkeleton(action:EngineAction)	extends ToSkeleton
