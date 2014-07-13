package djane.audio.decoder

import java.io._

import scala.util.matching.Regex

import scutil.implicits._
import scutil.lang._
import scutil.platform._
import scutil.log._

import djane.audio.Metadata

object Decoder extends Logging {
	val all:Seq[Decoder]	=
			Vector(
				ExternalMadplay,
				ExternalMpg123,
				ExternalFaad, 
				ExternalVorbisTools, 
				ExternalFlac,
				ExternalAvconv
			)
	
	def readMetadata(input:File):Option[Metadata]	= 
			worker { _ readMetadata input }
		
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Boolean	=
			(worker { _ convertToWav (input, output, frameRate, channelCount) }).isDefined

	private def worker[T](work:Decoder=>Tried[Seq[String],T]):Option[T] = {
		type Outcome	= Tried[Seq[Problem],T]
		type Problem	= Seq[String]
		
		def problem(decoder:Decoder, messages:Seq[String]):Seq[String]	=
				decoder.name +: messages
		
		val start:Outcome	= Fail(Seq.empty[Problem])
		val outcome:Outcome	=
				(all foldLeft start) { (outcome:Outcome,decoder) =>
					outcome match {
						case Fail(p)	=> work(decoder) match {
							case Fail(messages)	=> Fail(problem(decoder, messages) +: p)
							case Win(t)			=> Win(t)
						}
						case Win(t)		=> Win(t)
					}
				}
				
		outcome.swap.toVector.flatten foreach { it => WARN(it:_*) }
		outcome.toOption
	}
}

/** interface to an external decoder command */
trait Decoder extends Logging {
	type Checked[T]	= Tried[Seq[String],T]
	
	def name:String
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata]
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit]
	
	//------------------------------------------------------------------------------
	
	protected def requireChecked(cond:Boolean, problem:String):Checked[Unit]	=
			cond trueWin Vector(problem)
		
	protected def extractor(lines:Seq[String]):Regex=>Option[String]	=
			lines collapseFirst _.unapplySeq flatMap { _.headOption }
		
	protected def commandChecked(command:String):Checked[Unit]	=
			requireChecked(
				(External exec Vector("which", command) result false).rc == 0,
				s"command ${command} not available"
			)
		
	protected def suffixesChecked(suffixes:String*)(input:File):Checked[Unit]	=
			requireChecked(
				suffixes exists { input.getName.toLowerCase endsWith _ },
				"expected suffix in " + (suffixes mkString ", ")
			)
		
	protected def execChecked(command:String*):Checked[ExternalResult]	= {
		DEBUG(command:_*)
		External exec command result false triedBy { _.rc == 0 } mapFail { _.err }
	}
}
