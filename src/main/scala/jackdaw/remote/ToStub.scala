package jackdaw.remote

import jackdaw.player.*

enum ToStub {
	case Started(outputRate:Int, phoneEnabled:Boolean)
	case Send(feedback:EngineFeedback)
}
