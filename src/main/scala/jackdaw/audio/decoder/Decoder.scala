package jackdaw.audio.decoder

import java.io._

import scala.util.matching.Regex

import scutil.implicits._
import scutil.lang._
import scutil.platform._
import scutil.log._

import jackdaw.audio.Metadata

object Decoder extends Logging {
	val all:ISeq[Decoder]	=
			Vector(
				ExternalMadplay,
				ExternalMpg123,
				ExternalFaad, 
				ExternalVorbisTools, 
				ExternalFlac,
				ExternalAvconv,
				InternalJLayerMp3agic
			)
	
	def readMetadata(input:File):Option[Metadata]	= 
			worker { _ readMetadata input }
		
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Boolean	=
			(worker { _ convertToWav (input, output, frameRate, channelCount) }).isDefined

	private def worker[T](work:Decoder=>Tried[ISeq[String],T]):Option[T] = {
		type Outcome	= Tried[ISeq[Problem],T]
		type Problem	= ISeq[String]
		
		def problem(decoder:Decoder, messages:ISeq[String]):ISeq[String]	=
				decoder.name +: messages
		
		val start:Outcome	= Fail(ISeq.empty[Problem])
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
	def name:String
	
	/** read metadata */
	def readMetadata(input:File):Checked[Metadata]
	
	/** decode file and write into a wav file */
	def convertToWav(input:File, output:File, frameRate:Int, channelCount:Int):Checked[Unit]
	
	//------------------------------------------------------------------------------

	protected def extractor(lines:ISeq[String]):Regex=>Option[String]	=
			lines collapseFirst _.unapplySeq flatMap { _.headOption }
			
	//------------------------------------------------------------------------------
		
	// TODO does probably not work on windows
	protected final def commandAvailable(command:String):Checked[Unit]	=
			requirement(
				(External exec Vector("which", command) result false).rc == 0,
				s"command ${command} not available"
			)
		
	protected final def suffixIn(suffixes:String*)(input:File):Checked[Unit]	=
			requirement(
				suffixes exists { input.getName.toLowerCase endsWith _ },
				"expected suffix in " + (suffixes mkString ", ")
			)
		
	protected final def exec(command:String*):Checked[ExternalResult]	= {
		DEBUG(command:_*)
		External exec command.toVector result false triedBy { _.rc == 0 } mapFail { _.err }
	}
	
	protected final def requirement(cond:Boolean, problem:String):Checked[Unit]	=
			cond trueWin Vector(problem)
}
