package jackdaw.player

import java.util.concurrent.ConcurrentLinkedQueue

import scala.annotation._

import scutil.lang._

final class TransferQueue[T] {
	private val queue	= new ConcurrentLinkedQueue[T]
	
	def send(item:T) { queue offer item }
	def receive:Option[T]	= Option(queue.poll)
	
	def size	= queue.size
}
