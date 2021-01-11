package jackdaw.util

import scutil.core.implicits._
import scutil.lang._

object Checked {
	def trueWin1(cond:Boolean, problem:String):Checked[Unit]	=
		cond guardEither problem1(problem)

	def fail1[T](problem:String):Checked[T]	=
		Left(problem1(problem))

	def problem1(problem:String):Nes[String]	=
		Nes one problem
}
