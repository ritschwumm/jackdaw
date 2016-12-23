package jackdaw.remote

import scutil.core.implicits._
import scutil.lang._
import scutil.time._
import scutil.log._

import jackdaw.player._
import jackdaw.concurrent._

/** runs on the server */
object EngineSkeleton extends Logging {
	def main(args:Array[String]) {
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
					sendToStub(SendStub(engineFeedback))
					true
				}
			)
			
	private val engine			= new Engine(sender)
	private val tcpClient		= new TcpClient(port)
	private val tcpConnection:TcpConnection[ToSkeleton,ToStub]	= tcpClient.connect()
	
	def start() {
		DEBUG("starting")
		sendToStub(StartedStub(engine.outputRate, engine.phoneEnabled))
		
		engine.start()
		receiver.start()
		sender.start()
		DEBUG("started")
	}
	
	private def die() {
		DEBUG("disposing")
		// this is called from the receiver itself
		sender.dispose()
		engine.dispose()
		tcpConnection.dispose()
		DEBUG("disposed")
	}
	
	//------------------------------------------------------------------------------
	
	private val receiver	=
			Worker(
				"skeleton receiver",
				Thread.NORM_PRIORITY,
				thunk {
					receiveAndAct()
				}
			)
	
	private def receiveAndAct():Boolean	=
			try {
				tcpConnection.receive() match {
					case KillSkeleton			=> die();							false
					case SendSkeleton(action)	=> engine enqueueAction action;		true
				}
			}
			catch { case e:Exception =>
				ERROR("parent (stub) died unexpectedly, commiting suicide", e)
				sys exit 1
				nothing
			}
	
	//------------------------------------------------------------------------------
	
	private def sendToStub(msg:ToStub) {
		tcpConnection send msg
	}
}
