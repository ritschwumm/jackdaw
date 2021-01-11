package jackdaw.util

import scutil.core.implicits._
import scutil.lang._

import scala.collection.mutable

/** last recently used cache, unsynchronized */
final class LRU[S,T](size:Int, create:S=>T, touch:Effect[T] = (t:T)=>(), delete:Effect[T] = (t:T)=>()) extends AutoCloseable {
	private val keys	= mutable.Queue.empty[S]
	private val cache	= mutable.Map.empty[S,T]

	/** get some value, if possible form the cache */
	def load(s:S):T = cache get s match {
		case Some(t)	=>
			// move existing to the end of the queue
			keys dequeueFirst { _ ==== s}
			keys enqueue s
			touch(t)
			t
		case None	=>
			// prune if necessary
			if (keys.size >= size) {
				val	ss	= keys.dequeue()
				val tt	= cache(ss)
				cache remove ss
				delete(tt)
			}
			create(s) doto { t =>
				keys enqueue s
				cache(s)	= t
			}
	}

	/** remove items matching a predicate */
	def pruneIf(predicate:Predicate[S]):Unit	= {
		val	ks	= keys dequeueAll predicate
		val vs	= ks map cache
		cache --= ks
		vs foreach delete
	}

	def close():Unit	= {
		cache.values foreach delete
	}
}
