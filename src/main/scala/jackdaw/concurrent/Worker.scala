package jackdaw.concurrent

import scutil.lang._

object Worker {
	def apply(name:String, priority:Int, body:Io[Boolean]):Worker	=
		new WorkerThread(name, priority, body)
}

sealed trait Worker extends AutoCloseable {
	def start():Unit
	//def close():Unit
}

private final class WorkerThread(name:String, priority:Int,  body:Io[Boolean]) extends Thread with Worker {
	setName(name)
	setPriority(priority)

	@volatile
	private var keepAlive	= true

	def close():Unit	= {
		keepAlive	= false
		interrupt()
		join()
	}

	override protected def run():Unit	= {
		try {
			while (keepAlive) {
				if (!body.unsafeRun())	return
			}
		}
		catch { case e:InterruptedException =>
			// just exit
		}
	}
}
