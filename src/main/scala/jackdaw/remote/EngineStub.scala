package jackdaw.remote

import java.io._

import scutil.core.implicits._
import scutil.base.implicits._
import scutil.lang._
import scutil.platform._
import scutil.log._

import jackdaw.Config
import jackdaw.player._
import jackdaw.concurrent._

object EngineStub {
	// TODO find a better way of forking
	private object vm {
		val binary	=
			(new File(SystemProperties.java.home) / "bin" / "java").getCanonicalPath

		val userDir	=
			new File(SystemProperties.user.dir)

		val classpath	=
			SystemProperties.java.clazz.path

		val options	=
			Vector(
				"-server",
				"-Xms64M",
				"-Xmx64M",
				"-XX:+UseG1GC",
				"-XX:MaxGCPauseMillis=10",
				// switch off access to /tmp/hsperfdata_$USER which can cause IO
				"-XX:-UsePerfData",
				"-XX:+PerfDisableSharedMem",
				// "-XX:+PrintCompilation",
				// "-XX:+PrintGCApplicationStoppedTime",
				// "-XX:+PrintGCDetails",
			)

		val mainClass	=
			classOf[EngineSkeleton].getName
	}
}

/** runs on the client */
final class EngineStub extends Logging {
	INFO("starting audio server")

	private val tcpServer	= new TcpServer
	DEBUG("listening on", tcpServer.port)

	private val command		=
		Vector(EngineStub.vm.binary)	++
		EngineStub.vm.options			++
		Vector(
			"-cp",						EngineStub.vm.classpath,
			EngineStub.vm.mainClass,	tcpServer.port.toString
		)
	private val	builder		= new ProcessBuilder(command.toJList)
	builder directory		EngineStub.vm.userDir
	builder redirectOutput	ProcessBuilder.Redirect.INHERIT
	builder redirectError	ProcessBuilder.Redirect.INHERIT
	// builder.environment() putAll env.toJMap
	private val process			= builder.start()

	DEBUG("initializing communication")
	private val tcpConnection:TcpConnection[ToStub,ToSkeleton]	= tcpServer.connect()
	val (outputRate, phoneEnabled)	=
		tcpConnection.receive() match {
			case ToStub.Started(outputRate, phoneEnabled)	=> (outputRate, phoneEnabled)
			case x@ToStub.Send(_)							=> sys error show"unexpected message ${x.toString}"
		}
	DEBUG("output rate", outputRate)
	DEBUG("phone enabled", phoneEnabled)

	INFO("started audio server")

	//------------------------------------------------------------------------------

	def start():Unit	= {
		DEBUG("starting")
		// tcpServer is already started...
		receiver.start()
		DEBUG("started")
	}

	def dispose():Unit	= {
		DEBUG("disposing")
		receiver.dispose()

		sendToSkeleton(ToSkeleton.Kill)

		process.destroy()
		// avoid memory leak, see http://developer.java.sun.com/developer/qow/archive/68/
		process.waitFor()

		tcpConnection.dispose()
		DEBUG("disposed")
	}

	//------------------------------------------------------------------------------

	private val feedbackSmoothing	=
		new FeedbackSmoothing[EngineFeedback](
			initialFeedbackRate	= outputRate.toDouble / Config.guiIntervalFrames,
			overshotTarget		= Config.guiQueueOvershot,
			adaptFactor			= Config.guiQueueAdaptFactor
		)
	private val feedbackTarget		= feedbackSmoothing.asTarget

	def feedbackTimed(deltaNanos:Long):Option[EngineFeedback]	=
		feedbackSmoothing feedbackTimed deltaNanos

	def enqueueAction(action:EngineAction):Unit	= {
		sendToSkeleton(ToSkeleton.Send(action))
	}

	//------------------------------------------------------------------------------

	private val receiver	=
		Worker(
			"stub receiver",
			Thread.NORM_PRIORITY,
			Io delay {
				receiveAndAct()
			}
		)

	private def receiveAndAct():Boolean	=
		try {
			tcpConnection.receive() match {
				case ToStub.Send(feedback)	=> feedbackTarget send feedback;			true
				case x@ToStub.Started(_, _)	=> ERROR("unexpected message", x.toString);	false
			}
		}
		catch { case e:Exception =>
			ERROR("child (skeleton) died unexpectedly")
			false
		}

	private def sendToSkeleton(msg:ToSkeleton):Unit	= {
		tcpConnection send msg
	}
}
