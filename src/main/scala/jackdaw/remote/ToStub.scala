package jackdaw.remote

import jackdaw.player._

object ToStub {
	final case class Started(outputRate:Int, phoneEnabled:Boolean)	extends ToStub
	final case class Send(feedback:EngineFeedback)					extends ToStub
}

sealed trait ToStub
