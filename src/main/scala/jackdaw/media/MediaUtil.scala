package jackdaw.media

import java.lang.{ Iterable => JIterable }
import java.io.File

import scala.util.matching.Regex

import scutil.lang._
import scutil.implicits._
import scutil.platform._
import scutil.log._

object MediaUtil extends Logging {
	/** try one S after another to get a Win */
	def worker[S,T](all:ISeq[S], name:S=>String, work:S=>Tried[ISeq[String],T]):Option[T] = {
		type Outcome	= Tried[ISeq[Problem],T]
		type Problem	= ISeq[String]
		
		def problem(item:S, messages:ISeq[String]):ISeq[String]	=
				name(item) +: messages
		
		val start:Outcome	= Fail(ISeq.empty[Problem])
		val outcome:Outcome	=
				(all foldLeft start) { (outcome:Outcome, item) =>
					outcome match {
						case Fail(p)	=> work(item) match {
							case Fail(messages)	=> Fail(problem(item, messages) +: p)
							case Win(t)			=> Win(t)
						}
						case Win(t)		=> Win(t)
					}
				}
				
		outcome.swap.toVector.flatten foreach { it => WARN(it:_*) }
		outcome.toOption
	}
	
	//------------------------------------------------------------------------------
	
	def requireFileSuffixIn(suffixes:String*):File=>Checked[Unit]	=
			file =>
			Checked trueWin1 (
				suffixes exists { file.getName.toLowerCase endsWith _ },
				"expected suffix in " + (suffixes mkString ", ")
			)

	def requireCommand(command:String):Checked[Unit]	=
			OperatingSystem.current match {
				case Some(Linux) | Some(OSX)	=>
					Checked trueWin1 (
						(External exec Vector("which", command) result false).rc == 0,
						s"command ${command} not available"
					)
				case _ =>
					Checked fail1 "external media converters are only supported on OSX and Linux"
			}
			
	def runCommand(command:String*)(implicit sl:SourceLocation):Checked[ExternalResult]	= {
		DEBUG(command:_*)
		External exec command.toVector result false triedBy { _.rc == 0 } mapFail { _.err }
	}
	
	//------------------------------------------------------------------------------
	
	def checkedExceptions[T](block: =>Checked[T])(implicit sl:SourceLocation):Checked[T]	=
			(Catch.exception in block)
			.mapFail { e =>
				ERROR(e)
				Checked problem1 e.getMessage
			}
			.flatten

	//------------------------------------------------------------------------------
	
	def extractFrom(lines:ISeq[String]):Regex=>Option[String]	=
			lines collapseFirst _.unapplySeq flatMap { _.headOption }
}
