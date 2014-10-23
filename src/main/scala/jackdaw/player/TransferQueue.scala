package jackdaw.player

import java.util.concurrent.ConcurrentLinkedQueue

import scala.annotation._

import scutil.lang._

final class TransferQueue[T] {
	private val queue	= new ConcurrentLinkedQueue[T]
	
	def send(item:T) { queue offer item }
	def receive:Option[T]	= Option(queue.poll)
	
	@tailrec
	def receiveWith(effect:Effect[T]) {
		val item	= queue.poll
		if (item != null) {
			effect(item)
			receiveWith(effect)
		}
	}
	
	/*
	def receiveAll:ISeq[T]	= {
		var out	= Vector.empty[T]
		while  (true) {
			val item	= queue.poll
			if (item == null)	return out
			out	:+= item
		}
		nothing
	}
	*/
	
	def size	= queue.size
}
