package jackdaw

import scutil.gui.SwingUtil.edt
import scutil.platform.ExceptionUtil
import scutil.log._

object Boot extends Logging {
	def main(args:Array[String]) {
		System setProperty ("apple.awt.application.name", BuildInfo.name)
		
		ExceptionUtil logAllExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
		ExceptionUtil logAWTExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
	
		edt {
			try {
				Main
			}
			catch { case e:Exception =>
				ERROR("cannot start application", e)
			}
		}
	}
}
