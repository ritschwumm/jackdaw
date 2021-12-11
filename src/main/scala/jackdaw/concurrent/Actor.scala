package jackdaw.concurrent

import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit

import scutil.lang._
import scutil.time._
import scutil.concurrent._

object Actor {
	def create[T](name:String, priority:Int, parking:MilliDuration, body:T=>Boolean):IoResource[Target[T]]	=
		for {
			// NOTE this is uses a LinkedTransferQueue instead of a an ConcurrentLinkedQueue because
			// we _want_ to be able to block on reads when there is nothing available in the queue
			queue	<-	IoResource delay new LinkedTransferQueue[T]
			worker	<-	SimpleWorker.create(
							name,
							priority,
							Io delay {
								val message	= queue.poll(parking.millis, TimeUnit.MILLISECONDS)
								message == null || body(message)
							}
						)
		}
		yield queue offer _
}
