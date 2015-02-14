package jackdaw.gui.action

import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.border.Border

import scutil.implicits._

import screact._

import jackdaw.gui.util._

object KeyInput {
	def focusInput(enabled:Signal[Boolean], component:JComponent, off:Border, on:Border)(implicit observing:Observing):KeyInput	= {
		val hovered		= ComponentUtil underMouseSignal component
		val focussed	= (hovered zipWith enabled) { _ && _ }
		val border		= focussed map { _ cata (off, on) }
		border observeNow component.setBorder
		new KeyInput(focussed, fineModifier, keyDown)
	}
			
	//------------------------------------------------------------------------------
	
	private val fineModifier:Signal[Boolean]	=
			Keyboard.keys map { _ exists { _.code == KeyEvent.VK_SHIFT } }
		
	private def keyDown(key:Key):Signal[Boolean]	=
			Keyboard.keys map { _ contains key }
}


final class KeyInput(focussed:Signal[Boolean], fineModifier:Signal[Boolean], keyDown:Key=>Signal[Boolean]) {
	implicit class RichKey(peer:Key) {
		def asModifier:Signal[Boolean]	=
				(keyDown(peer) zipWith focussed) { _ && _ }
			
		def asAction:Events[Unit]	=
				keyDown(peer).edge.trueUnit gate focussed
	}
	
	//------------------------------------------------------------------------------
	
	// NOTE this is special because of the sequenceOptionSecond
	implicit class RichDirectionModifier1(peer:Signal[Option[Boolean]]) {
		def withFine:Signal[Option[(Boolean,Boolean)]]	=
				peer zip fineModifier map { _.first.sequenceOption }
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
