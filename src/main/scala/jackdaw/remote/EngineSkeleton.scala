package jackdaw.remote

import scutil.core.implicits._
import scutil.lang._
import scutil.time._
import scutil.log._

import jackdaw.player._
import jackdaw.concurrent._

/** runs on the server */
object EngineSkeleton extends Logging {
	def main(args:Array[String]):Unit	= {
		INFO("booting")
		val port		= args.head.toInt
		val skeleton	= new EngineSkeleton(port)
		skeleton.start()
	}

	private val cycleDelay:MilliDuration	= 5.millis
}

final class EngineSkeleton(port:Int) extends Logging {
	private val sender	=
		Actor[EngineFeedback](
			"skeleton sender",
			Thread.NORM_PRIORITY,
			EngineSkeleton.cycleDelay,
			engineFeedback	=> {
				sendToStub(ToStub.Send(engineFeedback))
				true
			}
		)

	private val engine		= new Engine(sender)
	private val tcpClient	= new TcpClient(port)
	private val tcpConnection:TcpConnection[ToSkeleton,ToStub]	= tcpClient.connect()

	def start():Unit	= {
		DEBUG("starting")
		sendToStub(ToStub.Started(engine.outputRate, engine.phoneEnabled))

		engine.start()
		receiver.start()
		sender.start()
		DEBUG("started")
	}

	private def die():Unit	= {
		DEBUG("disposing")
		sender.close()
		// this is called from the receiver itself
		engine.close()
		tcpConnection.close()
		DEBUG("disposed")
	}

	//------------------------------------------------------------------------------

	private val receiver	=
		Worker(
			"skeleton receiver",
			Thread.NORM_PRIORITY,
			Io delay {
				receiveAndAct()
			}
		)

	private def receiveAndAct():Boolean	=
		try {
			tcpConnection.receive() match {
				case ToSkeleton.Kill			=> die();							false
				case ToSkeleton.Send(action)	=> engine enqueueAction action;		true
			}
		}
		catch { case e:Exception =>
			ERROR("parent (stub) died unexpectedly, commiting suicide", e)
			sys exit 1
			nothing
		}

	//------------------------------------------------------------------------------

	private def sendToStub(msg:ToStub):Unit	= {
		tcpConnection send msg
	}
}
