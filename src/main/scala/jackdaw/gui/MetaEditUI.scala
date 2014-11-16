package jackdaw.gui

import java.awt.event._
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.CasterInstances._
import scutil.gui.SwingUtil._

import screact._
import screact.swing._

/** edits a single text cell */
final class MetaEditUI(value:Signal[Option[String]], strong:Boolean) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## input
	
	private val display:Signal[String]	=
			value map { _ getOrElse "" }
	
	private val editable:Signal[Boolean]	=
			value map { _.isDefined }
		
	//------------------------------------------------------------------------------
	//## components
	
	private val field	= new JTextField
	field	setFont			(strong	cata (Style.meta.edit.weak.font,	Style.meta.edit.strong.font))
	field	setBorder		null
	// file dropping should not focus this
	field	setDropTarget	null
	val component:JComponent	= field

	//------------------------------------------------------------------------------
	//## keyboard
	
	(component:KeyCaster) connect { ev =>
		component.getParent.guardNotNull foreach { parent =>
			import KeyEvent._
			(ev.getID, ev.getKeyCode) match {
				case (KEY_PRESSED, 	VK_ESCAPE)	=> parent.requestFocusInWindow()
				case (KEY_PRESSED, 	VK_ENTER)	=> parent.requestFocusInWindow()
				case _	=>
			}
		}
	}
	
	//------------------------------------------------------------------------------
	//## wiring

	private val textChanges:Events[String]	=
			SwingWidget transformer (
					display,
					(field.getDocument:DocumentCaster).connect,
					thunk { field.getText },
					field.setText)
		
	editable observeNow { it =>
		field setEditable	it
		field setFocusable	it
	}
	
	field onFocusGained	{ _ =>
		edt {
			field.selectAll()
		}
	}
	
	//------------------------------------------------------------------------------
	//## actions
	
	def startEditing() {
		field.requestFocusInWindow()
	}
	
	//------------------------------------------------------------------------------
	//## output
	
	val focussed:Signal[Boolean]	= 
			SwingWidget signal ( 
					(component:FocusCaster).connect,
					thunk { component.hasFocus })
			
	val changes:Events[String]	=
			textChanges gate editable
}
