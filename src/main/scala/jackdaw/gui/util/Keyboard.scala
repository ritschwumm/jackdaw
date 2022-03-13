package jackdaw.gui.util

import java.util.concurrent.atomic.AtomicReference
import java.awt.AWTEvent
import java.awt.event.*
import javax.swing.Timer

import scutil.core.implicits.*
import scutil.lang.*
import scutil.gui.GlobalAWTEvent
import scutil.gui.ListenerInstances.*

import screact.*

// TODO num lock leads to keys always held

object Keyboard {
	private val maxAge		= 10

	val create:IoResource[Signal[Set[Key]]]	=
		for {
			keysCell	<-	IoResource.unsafe.releasable(cell(Set.empty[Key]))
			state		=	new AtomicReference(Map.empty[Key,Option[Long]])
			update		= 	(now:Long) => {
								keysCell set state.updateAndGet{ state =>
									state filter {
										case (key, Some(when))	=> now - when < maxAge
										case (key, None)		=> true
									}
								}.keySet
							}
			_				<-	globalEvent {
									case ev:KeyEvent	=>
										val when	= ev.getWhen
										val key		= Key(ev.getKeyCode, ev.getKeyLocation)
										if (ev.getID == KeyEvent.KEY_PRESSED) {
											state updateAndGet { _ + (key -> None) }
										}
										else if (ev.getID == KeyEvent.KEY_RELEASED) {
											state updateAndGet { _ + (key -> Some(when)) }
										}
										update(when)
									case _	=>
										// ignored
								}
			_				<-	timer(
									maxAge/2,
									ev => update(ev.getWhen)
								)
		}
		yield keysCell.signal

	private def globalEvent(action:AWTEvent=>Unit):IoResource[Unit]	=
		IoResource.unsafe.releasable(GlobalAWTEvent.connect(AWTEvent.KEY_EVENT_MASK)(action)).void

	private def timer(delay:Int, action:ActionEvent=>Unit):IoResource[Unit]	=
		IoResource.unsafe.disposing {
			new Timer(delay, mkActionListener(action)).doto(_.start())
		}{
			_.stop()
		}
		.void
}
