package jackdaw.remote

import java.io._

import scutil.lang._
import scutil.implicits._
import scutil.platform._
import scutil.log._

import jackdaw.Config
import jackdaw.player._
import jackdaw.concurrent._

object EngineStub {
	private object vm {
		val binary	=
				(new File(SystemProperties.java.home) / "bin" / "java").getCanonicalPath
			
		val userDir	=
				new File(SystemProperties.user.dir)
			
		val classpath	=
				SystemProperties.java.clazz.path
			
		val options	=
				Vector(
					"-client",
					"-Xms64M",
					"-Xmx64M",
					"-XX:+UseG1GC",
					"-XX:MaxGCPauseMillis=10"
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
			EngineStub.vm.options		++
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
	private val tcpConnection	= tcpServer.connect()
	val (outputRate, phoneEnabled)	=	
			tcpConnection.receive() match {
				case StartedStub(outputRate, phoneEnabled)	=> (outputRate, phoneEnabled)
				case x										=> sys error so"unexpected message ${x.toString}"
			}
	DEBUG("output rate", outputRate)
	DEBUG("phone enabled", phoneEnabled)
	
	INFO("started audio server")
	
	//------------------------------------------------------------------------------
	
	def start() {
		DEBUG("starting")
		// tcpServer is already started...
		receiver.start()
		DEBUG("started")
	}
	
	def dispose() {
		DEBUG("disposing")
		receiver.dispose()
		
		sendToSkeleton(KillSkeleton)
		
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
	
	def enqueueAction(action:EngineAction) {
		sendToSkeleton(SendSkeleton(action))
	}
	
	//------------------------------------------------------------------------------
	
	private val receiver	=
			Worker(
				"stub receiver",
				Thread.NORM_PRIORITY,
				thunk {
					receiveAndAct()
				}
			)
	
	private def receiveAndAct():Boolean	=
			try {
				tcpConnection.receive() match {
					case SendStub(feedback)	=> feedbackTarget send feedback;	true
					case x					=> ERROR("unexpected message", x);	false
				}
			}
			catch { case e:Exception =>
				ERROR("child (skeleton) died unexpectedly")
				false
			}
	
	private def sendToSkeleton(msg:ToSkeleton) {
		tcpConnection send msg
	}
}
