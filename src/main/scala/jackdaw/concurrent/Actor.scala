package jackdaw.concurrent

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.LockSupport

import scutil.lang._
import scutil.time._

object Actor {
	def apply[T](name:String, priority:Int, parking:MilliDuration, body:T=>Boolean):Actor[T]	=
			new ActorThread[T](name, priority, parking, body)
}

sealed trait Actor[T] extends Target[T] with Disposable {
	def start():Unit
	def dispose():Unit
	def send(message:T):Unit
	def asTarget:Target[T]	= this
}

private final class ActorThread[T](name:String, priority:Int, parking:MilliDuration, body:T=>Boolean) extends Thread with Actor[T] {
	setName(name)
	setPriority(priority)
	
	private val queue			= new ConcurrentLinkedQueue[T]
	private val parkingNanos	= parking.nanos
	
	@volatile 
	private var keepAlive	= true

	def dispose() {
		keepAlive	= false
		interrupt()
		join()
	}
	
	final def send(message:T) {
		queue offer message
		LockSupport unpark this
	}
	
	override protected def run() {
		try {
			while (keepAlive) {
				if (!drain())	return
				if (keepAlive)	LockSupport parkNanos parkingNanos
			}
		}
		catch { case e:InterruptedException =>
			// just exit
		}
	}
	
	private def drain():Boolean	= {
		while (true) {
			val message	= queue.poll
			if (message == null)	return true
			if (!body(message))		return false
		}
		nothing
	}
}
