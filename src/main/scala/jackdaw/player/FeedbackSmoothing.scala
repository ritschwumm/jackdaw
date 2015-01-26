package jackdaw.player

import scala.math._

import jackdaw.concurrent._

final class FeedbackSmoothing[T](initialFeedbackRate:Double, overshotTarget:Int, adaptFactor:Double) {
	private val queue	= new Transfer[T]
	
	// block rate adapted to nanoTime jitter
	private var feedbackRate	= initialFeedbackRate
	
	val asTarget:Target[T]	= queue.asTarget
	 
	def feedbackTimed(deltaNanos:Long):Option[T]	= {
		val blocks	= round(deltaNanos.toDouble * feedbackRate / 1000000000D).toInt
		
		// BETTER exponential smoothing of diff
		
		// smooth nanoTime jitter
		val overshot	= queue.available - blocks
		val diff		= overshot - overshotTarget
		val shaped		= tanh(diff.toDouble / overshotTarget)
		val factor		= pow(adaptFactor, shaped)
		feedbackRate	= feedbackRate * factor
		
		var block	= 0
		var old		= None:Option[T]
		while (block < blocks) {
			val cur	= queue.receive
			if (cur == None) {
				return old
			}
			old		= cur
			block	+= 1
		}
		old
	}
}
