package jackdaw.gui.util

import java.awt.event.KeyEvent

import screact.*

final class KeyInput(keys:Signal[Set[Key]], target:Signal[Boolean]) {
	private val fineModifier:Signal[Boolean]	=
		keys map { _ exists { _.code == KeyEvent.VK_SHIFT } }

	private def keyDown(key:Key):Signal[Boolean]	=
		keys map { _ contains key }

	//------------------------------------------------------------------------------

	extension(peer:Key) {
		def asModifier:Signal[Boolean]	=
			(keyDown(peer) map2 target) { _ && _ }

		def asAction:Events[Unit]	=
			keyDown(peer).edge.trueUnit gate target
	}

	//------------------------------------------------------------------------------

	// NOTE this is special because of the sequenceOptionSecond (???)
	extension(peer:Signal[Option[Boolean]]) {
		def withFine:Signal[Option[(Boolean,Boolean)]]	= {
			peer product fineModifier map { case (opt,full) =>
				opt.map(_ -> full)
			}
		}
	}

	extension(peer:Events[Unit]) {
		def unitWithFine:Events[Boolean]	=
			peer snapshotOnly fineModifier
	}

	extension[T](peer:Events[T]) {
		def withFine:Events[(T,Boolean)]	=
			peer snapshot fineModifier
	}
}
