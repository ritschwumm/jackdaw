package djane

import scutil.gui.SwingUtil.edt
import scutil.platform.ExceptionUtil
import scutil.log._

object DJane extends Logging {
	def main(args:Array[String]) {
		ExceptionUtil logAllExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
		ExceptionUtil logAWTExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
	
		edt {
			try {
				DJaneMain
			}
			catch { case e:Exception =>
				ERROR("cannot start application", e)
			}
		}
	}
}
