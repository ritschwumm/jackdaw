package jackdaw.gui.util

import java.awt.AWTEvent
import java.awt.event.KeyEvent
import javax.swing.Timer

import scutil.lang._
import scutil.implicits._
import scutil.gui.GlobalAWTEvent
import scutil.gui.ListenerInstances._

import screact._

// TODO num lock leads to keys always held

object Keyboard extends Disposable {
	//------------------------------------------------------------------------------
	//## implementation
	
	private val maxAge		= 10
	
	private val keysCell	= cell(Set.empty[Key])
	
	// None means pressed, Some means released at that time
	private var state		= Map.empty[Key,Option[Long]]
	
	private def update(now:Long) {
		state	= state filter { 
			case (key, Some(when))	=> now - when < maxAge
			case (key, None)		=> true
		}
		keysCell set state.keySet
	}
	
	private val connection	=
			(GlobalAWTEvent connect AWTEvent.KEY_EVENT_MASK) {
				_ match { 
					case ev:KeyEvent	=>
						val when	= ev.getWhen
						val key		= Key(ev.getKeyCode, ev.getKeyLocation)
						if (ev.getID == KeyEvent.KEY_PRESSED) {
							state	+= (key -> None)
						}
						else if (ev.getID == KeyEvent.KEY_RELEASED) {
							state	+= (key -> Some(when))
						}
						update(when)
					case _	=>
						// ignored
				}
			}
	
	private val timer	= 
			new Timer(maxAge/2, mkActionListener { ev =>
				val when	= ev.getWhen
				update(when)
			})
	timer.start()
	
	//------------------------------------------------------------------------------
	//## public api
	
	val keys	= keysCell.signal
	
	def dispose() {
		timer.stop()
		connection.dispose()
		// NOTE are those necessary?
		keys.dispose()
		keysCell.dispose()
	}
}
