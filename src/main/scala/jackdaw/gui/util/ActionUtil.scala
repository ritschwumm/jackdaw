package jackdaw.gui.util

import scutil.core.implicits._
import scutil.lang._

import screact._
import screact.swing.SwingClock

object ActionUtil {
	//------------------------------------------------------------------------------
	//## repeats

	private val repeatDelay	= 300.millis
	private val repeatTick	= 100.millis

	implicit class RichRepeatable[T](peer:Signal[Option[T]]) {
		def repeated:Events[T]	=
			SwingClock.repeat(repeatTick, repeatDelay, peer.edge)
	}

	//------------------------------------------------------------------------------
	//## modifiers

	// BETTER use Option[Unit] here?
	implicit class RichModifier(peer:Signal[Boolean]) {
		def orElse(that:Signal[Boolean]):Signal[Boolean]	=
			(peer map2 that) { _ || _ }

		def upDown(that:Signal[Boolean]):Signal[Option[Boolean]]	=
			(peer map2 that)(directionValue)
	}

	implicit class RichDirectionModifier(peer:Signal[Option[Boolean]]) {
		def merge(that:Signal[Option[Boolean]]):Signal[Option[Boolean]]	=
			(peer map2 that)(mergeDirections)

		def steps:Signal[Option[Int]]	=
			peer map { _ map directionSteps }
	}

	//------------------------------------------------------------------------------
	//## actions

	implicit class RichAction(peer:Events[Unit]) {
		def upDown(that:Events[Unit]):Events[Boolean]	=
			(peer	tag true)	orElse
			(that	tag false)
	}

	implicit class RichDirectionAction(peer:Events[Boolean]) {
		def steps:Events[Int]	=
			peer map directionSteps
	}

	//------------------------------------------------------------------------------
	//## triggers

	implicit class RichUnitAction(peer:Events[Unit]) {
		def trigger(target:()=>Unit)(implicit obs:Observing):Unit	= {
			peer observe ignorant(target)
		}
	}

	implicit class RichPairAction[S,T](peer:Events[(S,T)]) {
		def trigger(target:(S,T)=>Unit)(implicit obs:Observing):Unit	= {
			peer observe target.tupled
		}
	}

	//------------------------------------------------------------------------------
	//## calculations

	private def directionSteps(up:Boolean):Int	=
		up.cata(-1, +1)

	private def directionValue(up:Boolean, down:Boolean):Option[Boolean]	=
		(up		option true) orElse
		(down	option false)

	private def mergeDirections(a:Option[Boolean], b:Option[Boolean]):Option[Boolean]	=
		(a, b) match {
			case (Some(true),	Some(true))		=> Some(true)
			case (Some(true),	None)			=> Some(true)
			case (Some(true),	Some(false))	=> None
			case (None,			Some(true))		=> Some(true)
			case (None,			None)			=> None
			case (None,			Some(false))	=> Some(false)
			case (Some(false),	Some(true))		=> None
			case (Some(false),	None)			=> Some(false)
			case (Some(false),	Some(false))	=> Some(false)
		}
}
