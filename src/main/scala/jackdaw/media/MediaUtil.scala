package jackdaw.media

import java.io.File

import scala.util.matching.Regex

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.lang._
import scutil.platform._
import scutil.log._

import jackdaw.util.Checked

object MediaUtil extends Logging {
	/** try one S after another to get a Win */
	def worker[S,T](all:ISeq[S], name:S=>String, work:S=>Checked[T]):Option[T] = {
		type Outcome	= Tried[ISeq[Group],T]
		
		final case class Group(worker:String, messages:Nes[String])
		
		val start:Outcome	= Fail(ISeq.empty[Group])
		val outcome:Outcome	=
				(all foldLeft start) { (outcome:Outcome, item) =>
					outcome match {
						case Win(t)	=>
							// already won, no need to keep on
							Win(t)
						case Fail(p)	=>
							// try next worker
							work(item) match {
								case Fail(messages)	=>
									// record the failure
									Fail(p :+ Group(name(item), messages))
								case Win(t)	=>
									Win(t)
							}
					}
				}
				
		val errorGroups:ISeq[Group]	= outcome.swap.flattenMany
		errorGroups foreach { it =>
			it.worker +: (it.messages map { "\t" + _ }) foreach { WARN(_) }
		}
		
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
						so"command ${command} not available"
					)
				case _ =>
					Checked fail1 "external media converters are only supported on OSX and Linux"
			}
			
	def runCommand(command:String*)(implicit sl:SourceLocation):Checked[ExternalResult]	= {
		DEBUG(command:_*)
		External exec command.toVector result false triedBy { _.rc == 0 } mapFail { res =>
			val first	= "command failed: " + (command mkString " ")
			Nes(first, res.err)
		}
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
			lines collapseMapFirst _.unapplySeq flatMap { _.headOption }
}
