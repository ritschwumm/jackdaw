package jackdaw.model

import java.io.File

import scala.math._

import scutil.base.implicits._
import scutil.lang._

import screact._

import jackdaw.range._
import jackdaw.range.PitchMath._
import jackdaw.data._
import jackdaw.persistence._
import jackdaw.persistence.JsonProtocol._

object Speed {
	private val step:Boolean=>Double	= _ cata (bpm(1),		bpm(0.05))
	private val drag:Boolean=>Double	= _ cata (cents(200),	cents(100))
}

/** metronome model */
final class Speed extends Observing {
	// in beats per second, SpeedRange
	private val valueCell:Cell[Double]				= cell(Rhythm.defaultBeatsPerSecond)
	val dragging:Cell[Option[(Boolean,Boolean)]]	= cell(None)

	// in beats per second
	val value:Signal[Double]	= valueCell

	def setValueRastered(it:Double, fine:Boolean):Unit	= {
		val step		= Speed step fine
		val rastered	= rint(it / step) * step
		valueCell set rastered
	}

	def moveSteps(steps:Int, fine:Boolean):Unit	= {
		valueCell modify modifier(steps, fine)
	}

	private def modifier(steps:Int, fine:Boolean):Endo[Double]	=
			it => SpeedRange clamp (it + steps * (Speed step fine))

	val beatRate:Signal[Double]	=
			signal {
				value.current * (
					dragging.current match {
						case None					=> 1.0
						case Some((false,	fine))	=> 1 / (Speed drag fine)
						case Some((true,	fine))	=> 1 * (Speed drag fine)
					}
				)
			}

	def persist(file:File):Unit	= {
		val persister	= new JsonPersister[Double]

		persister load file foreach {
			setValueRastered (_, true)
		}

		value observe {
			persister save file
		}
	}
}
