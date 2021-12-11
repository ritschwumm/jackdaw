package jackdaw.remote

import java.io._

import scutil.jdk.implicits._
import scutil.core.implicits._
import scutil.lang._
import scutil.platform._
import scutil.log._
import scutil.concurrent._

import jackdaw.Config
import jackdaw.player._
import jackdaw.concurrent.Target

object EngineStub extends Logging {
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

	//------------------------------------------------------------------------------

	val create:IoResource[EngineStub]	=
		for {
			_					<-	infoLifecycle("starting audio server", "audio server stopped")
			tcpServer			<-	TcpServer.create
			_					=	DEBUG("listening on", tcpServer.port)
			_					<-	engineProcess(tcpServer.port)
			_					=	DEBUG("initializing communication")
			tcpConnection		<-	tcpServer.connect
			sendToSkeleton		=	tcpConnection send (_:ToSkeleton)
			tmp					=	tcpConnection.receive() match {
										case ToStub.Started(outputRate, phoneEnabled)	=> (outputRate, phoneEnabled)
										case x@ToStub.Send(_)							=> sys error show"unexpected message ${x.toString}"
									}
			(outputRate, headphones)	= tmp
			_					=	DEBUG("output rate", outputRate)
			_					=	DEBUG("phone enabled", headphones)
			feedbackSmoothing	<-	IoResource delay {
										new FeedbackSmoothing[EngineFeedback](
											initialFeedbackRate	= outputRate.toDouble / Config.guiIntervalFrames,
											overshotTarget		= Config.guiQueueOvershot,
											adaptFactor			= Config.guiQueueAdaptFactor
										)
									}
			_					<-	SimpleWorker.create(
										"stub receiver",
										Thread.NORM_PRIORITY,
										Io delay {
											receiveAndAct(tcpConnection, feedbackSmoothing.asTarget)
										}
									)
			_					<-	IoResource.unsafe.afterwards(sendToSkeleton(ToSkeleton.Kill))
			_					<-	infoLifecycle("audio server started", "stopping audio server")
		}
		yield new EngineStub {
			val phoneEnabled	= headphones

			def feedbackTimed(deltaNanos:Long):Option[EngineFeedback]	=
				feedbackSmoothing feedbackTimed deltaNanos

			def enqueueAction(action:EngineAction):Unit	=
				sendToSkeleton(ToSkeleton.Send(action))
		}

	private def engineProcess(port:Int):IoResource[Unit]	= {
		val command:Vector[String]		=
			Vector(EngineStub.vm.binary)	++
			EngineStub.vm.options			++
			Vector(
				"-cp",						EngineStub.vm.classpath,
				EngineStub.vm.mainClass,	port.toString
			)
		val	builder		= new ProcessBuilder(command.toJList)
		builder directory		EngineStub.vm.userDir
		builder redirectOutput	ProcessBuilder.Redirect.INHERIT
		builder redirectError	ProcessBuilder.Redirect.INHERIT
		// builder.environment() putAll env.toJMap

		IoResource.unsafe.disposing {
			builder.start()
		}{ process =>
			process.destroy()
			// avoid memory leak, see http://developer.java.sun.com/developer/qow/archive/68/
			process.waitFor()
		}
		.void
	}

	private def receiveAndAct(tcpConnection:TcpConnection[ToStub,ToSkeleton], feedbackTarget:Target[EngineFeedback]):Boolean	=
		try {
			tcpConnection.receive() match {
				case ToStub.Send(feedback)	=> feedbackTarget send feedback;			true
				case x@ToStub.Started(_, _)	=> ERROR("unexpected message", x.toString);	false
			}
		}
		catch { case e:Exception =>
			// TODO using this occurs on disposal, where it is not actually unexpected -
			// we explicitly _told_ the engine process to die with a ToSkeleton.Kill
			ERROR("child (skeleton) died unexpectedly")
			false
		}

	private def infoLifecycle(start:String, end:String):IoResource[Unit]	=
		IoResource.unsafe.lifecycle(INFO(start), INFO(end))
}

/** runs on the client */
trait EngineStub {
	def phoneEnabled:Boolean
	def feedbackTimed(deltaNanos:Long):Option[EngineFeedback]
	def enqueueAction(action:EngineAction):Unit
}
