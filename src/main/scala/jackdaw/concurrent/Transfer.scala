package jackdaw.concurrent

import java.util.concurrent.ConcurrentLinkedQueue

import scala.annotation._

import scutil.lang._

final class Transfer[T] extends Target[T] {
	private val queue	= new ConcurrentLinkedQueue[T]

	def send(item:T):Unit	= {
		queue offer item
	}

	def asTarget:Target[T]	= this

	def available:Int	= queue.size

	def receive():Option[T]	= Option(queue.poll)

	@tailrec
	def receiveAll(effect:Effect[T]):Unit	= {
		val item	= queue.poll
		if (item != null) {
			effect(item)
			receiveAll(effect)
		}
	}
}
