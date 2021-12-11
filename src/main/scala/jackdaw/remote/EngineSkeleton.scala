package jackdaw.remote

import scutil.core.implicits._
import scutil.lang._
import scutil.time._
import scutil.log._

import scaudio.output._

import jackdaw.Config
import jackdaw.player._
import jackdaw.concurrent._

/** runs on the server */
object EngineSkeleton extends Logging {
	def main(args:Array[String]):Unit	= {
		INFO("booting")
		val port	= args.head.toInt
		create(port).use(identity).unsafeRun()
	}

	private val cycleDelay:MilliDuration	= 5.millis

	def create(port:Int):IoResource[Io[Unit]]	= {
		for {
			_				<-	debugLifecycle("starting", "stopped")
			tcpConnection	<-	TcpClient.open(port)
			sendToStub		=	tcpConnection send (_:ToStub)
			sender			<-	Actor.create[EngineFeedback](
									"skeleton sender",
									Thread.NORM_PRIORITY,
									EngineSkeleton.cycleDelay,
									engineFeedback	=> {
										sendToStub(ToStub.Send(engineFeedback))
										true
									}
								)
			output			=	Output.find(Config.outputConfig).getOrError("audio is not available")
			_				<-	IoResource delay sendToStub(ToStub.Started(output.outputInfo.rate, output.outputInfo.headphone))
			enqueueAction	<-	Engine.create(output, sender)
			_				<-	debugLifecycle("started", "stopping")
		}
		yield Io delay {
			// TODO using move this out
			var keepOn	= true
			while (keepOn) {
				try {
					tcpConnection.receive() match {
						case ToSkeleton.Send(action)	=> enqueueAction(action)
						case ToSkeleton.Kill			=> keepOn	= false
					}
				}
				catch { case e:Exception =>
					ERROR("parent (stub) died unexpectedly, commiting suicide", e)
					sys exit 1
					nothing
				}
			}
		}
	}

	private def debugLifecycle(start:String, end:String):IoResource[Unit]	=
		IoResource.unsafe.lifecycle(DEBUG(start), DEBUG(end))
}

// NOTE this is just here for the static forwarder
class EngineSkeleton
