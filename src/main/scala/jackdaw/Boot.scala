package jackdaw

import scutil.core.implicits._
import scutil.lang._
import scutil.gui.SwingUtil.edt
import scutil.platform.ExceptionUtil
import scutil.log._

object Boot extends Logging {
	def main(args:Array[String]):Unit	= {
		System.setProperty("apple.awt.application.name", BuildInfo.name)

		ExceptionUtil logAllExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
		ExceptionUtil logAWTExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}

		edt {
			try {
				var disposer:Io[Unit]	= Io.unit

				val shutdown:Io[Unit]	=
					Io delay {
						disposer.unsafeRun()

						// NOTE ugly, but necessary because Main$ is referenced from Thread#contextClassLoader and there are some additional GC roots still alive in swing
						sys exit 0
					}

				val main	= Main.create(shutdown).open.unsafeRun()
				disposer	= main._2
			}
			catch { case e:Exception =>
				ERROR("cannot start application", e)
			}
		}
	}
}
