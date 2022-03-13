package jackdaw.gui

import java.awt.event.*
import javax.swing.*

import scutil.core.implicits.*
import scutil.lang.*
import scutil.gui.CasterInstances.*
import scutil.gui.SwingUtil.*

import screact.*
import screact.swing.*

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
	field	setFont			strong.cata(Style.meta.edit.weak.font,	Style.meta.edit.strong.font)
	field	setBorder		null
	// file dropping should not focus this
	field	setDropTarget	null

	val component:JComponent	= field
	component.putClientProperty("STRONG_REF", this)

	//------------------------------------------------------------------------------
	//## keyboard

	(component:KeyCaster) connect { ev =>
		component.getParent.optionNotNull foreach { parent =>
			import KeyEvent.*
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
		SwingWidget.transformer(
			display,
			(field.getDocument:DocumentCaster).connect,
			thunk { field.getText },
			field.setText
		)

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

	def startEditing():Unit	= {
		field.requestFocusInWindow()
	}

	//------------------------------------------------------------------------------
	//## output

	val focussed:Signal[Boolean]	=
		SwingWidget.signal(
			(component:FocusCaster).connect,
			thunk { component.hasFocus }
		)

	val changes:Events[String]	=
		textChanges gate editable
}
