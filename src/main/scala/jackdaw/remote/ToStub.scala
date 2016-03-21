package jackdaw.remote

import jackdaw.player._

sealed trait ToStub

final case class StartedStub(outputRate:Int, phoneEnabled:Boolean)	extends ToStub
final case class SendStub(feedback:EngineFeedback)					extends ToStub
