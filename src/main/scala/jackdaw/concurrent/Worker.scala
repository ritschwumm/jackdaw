package jackdaw.concurrent

import scutil.lang._

object Worker {
	def apply(name:String, priority:Int, body:Thunk[Boolean]):Worker	=
			new WorkerThread(name, priority, body)
}

sealed trait Worker extends Disposable {
	def start():Unit
	def dispose():Unit
}

private final class WorkerThread(name:String, priority:Int,  body:Thunk[Boolean]) extends Thread with Worker {
	setName(name)
	setPriority(priority)
	
	@volatile
	private var keepAlive	= true

	def dispose() {
		keepAlive	= false
		interrupt()
		join()
	}
	
	override protected def run() {
		try {
			while (keepAlive) {
				if (!body())	return
			}
		}
		catch { case e:InterruptedException =>
			// just exit
		}
	}
}
