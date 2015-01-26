package jackdaw.remote

import jackdaw.player._

sealed trait ToStub

case class StartedStub(outputRate:Int, phoneEnabled:Boolean)	extends ToStub
case class SendStub(feedback:EngineFeedback)					extends ToStub
