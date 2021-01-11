package jackdaw.gui.util

import java.awt.event.KeyEvent

import screact._

object KeyInput {
	def when(target:Signal[Boolean]):KeyInput	=
		new KeyInput(target, Keyboard.keys)
}

final class KeyInput(target:Signal[Boolean], keys:Signal[Set[Key]]) {
	private val fineModifier:Signal[Boolean]	=
		keys map { _ exists { _.code == KeyEvent.VK_SHIFT } }

	private def keyDown(key:Key):Signal[Boolean]	=
		keys map { _ contains key }

	//------------------------------------------------------------------------------

	implicit class RichKey(peer:Key) {
		def asModifier:Signal[Boolean]	=
			(keyDown(peer) map2 target) { _ && _ }

		def asAction:Events[Unit]	=
			keyDown(peer).edge.trueUnit gate target
	}

	//------------------------------------------------------------------------------

	// NOTE this is special because of the sequenceOptionSecond (???)
	implicit class RichDirectionModifier1(peer:Signal[Option[Boolean]]) {
		def withFine:Signal[Option[(Boolean,Boolean)]]	= {
			peer product fineModifier map { case (opt,full) =>
				opt.map(_ -> full)
			}
		}
	}

	implicit class RichAction1(peer:Events[Unit]) {
		def withFine:Events[Boolean]	=
			peer snapshotOnly fineModifier
	}

	implicit class RichAction2[T](peer:Events[T]) {
		def withFine:Events[(T,Boolean)]	=
			peer snapshot fineModifier
	}
}
