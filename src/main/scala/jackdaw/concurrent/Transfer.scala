package jackdaw.concurrent

import java.util.concurrent.ConcurrentLinkedQueue

import scala.annotation._

import scutil.lang._

final class Transfer[T] extends Target[T] {
	private val queue	= new ConcurrentLinkedQueue[T]
	
	def send(item:T) {
		queue offer item
	}
	
	def available:Int	= queue.size
	
	def receive():Option[T]	= Option(queue.poll)
	
	@tailrec
	def receiveAll(effect:Effect[T]) {
		val item	= queue.poll
		if (item != null) {
			effect(item)
			receiveAll(effect)
		}
	}
}
