package jackdaw.concurrent

import java.util.concurrent.ConcurrentLinkedQueue

import scala.annotation._

import scutil.lang._

// NOTE drone.concurrent.TransferQueue and  jackdaw.concurrent.Transfer are almost identical
final class Transfer[T] {
	private val queue	= new ConcurrentLinkedQueue[T]

	def available:Int		= queue.size
	def send(item:T):Unit	= queue offer item
	def receive():Option[T]	= Option(queue.poll)

	@tailrec
	def receiveAll(effect:Effect[T]):Unit	= {
		val item	= queue.poll
		if (item != null) {
			effect(item)
			receiveAll(effect)
		}
	}

	def asTarget:Target[T]	= message => send(message)
}
