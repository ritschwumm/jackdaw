package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._
import java.awt.event._
import java.util.Locale

import scutil.implicits._
import scutil.gui.implicits._
import scutil.gui.GridBagDSL._

import screact._

import jackdaw.model._
import jackdaw.gui.action._
import jackdaw.gui.util._

import GridBagItem.UI_is_GridBagItem

final class SpeedUI(speed:Speed, keyboardEnabled:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## input
	
	private val speedString	= speed.value map Render.bpm
	
	//------------------------------------------------------------------------------
	//## components
	
	private val speedEditor	= UIFactory	speedLinear	speed.value
	speedEditor.component	setAllSizes	Style.linear.size
	
	private val speedDisplay	= new JLabel{
		override def getPreferredSize	=
				super.getPreferredSize |>> { _.width = Style.speed.width }
	}
	speedDisplay setForeground			Style.speed.display.color
	speedDisplay setFont				Style.speed.display.font
	speedDisplay setHorizontalAlignment	SwingConstants.RIGHT
	
	private val	pullButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.LEFT),	static(true))
	private val	pushButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RIGHT),	static(true))
	private val	downButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.MINUS),	static(true))
	private val	upButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.PLUS),	static(true))
	
	private val buttonPanel	=
			HBoxUI(
				pullButton,
				BoxStrut(4),
				pushButton,
				BoxStrut(4+4),
				downButton,
				BoxStrut(4),
				upButton
			)
	
	private val panel	=
			GridBagUI(
				speedEditor		pos(0,0) size(2,1) weight(1,1) fill HORIZONTAL	anchor CENTER	insetsTLBR(0,0,2,0),
				buttonPanel		pos(0,1) size(1,1) weight(0,1) fill NONE		anchor WEST		insetsTLBR(2,4,0,4),
				speedDisplay	pos(1,1) size(1,1) weight(0,1) fill NONE 		anchor EAST		insetsTLBR(3,4,0,4)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	import KeyEvent._
	
	private val focusInput	= 
			KeyInput focusInput (
				enabled		= keyboardEnabled,
				component	= component,
				off			= Style.speed.border.noFocus,
				on			= Style.speed.border.inFocus
			)
	import focusInput._
	
	import ActionUtil._
	
	// modifiers
	
	private val draggingKey:Signal[Option[Boolean]]	=
			Key(VK_UP,		KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_DOWN,	KEY_LOCATION_STANDARD).asModifier
	private val draggingButton:Signal[Option[Boolean]]	=
			pushButton.pressed	upDown 
			pullButton.pressed
	private val dragging:Signal[Option[Boolean]]	=
			draggingKey	merge
			draggingButton
	dragging.withFine observeNow speed.dragging.set
	
	// actions
	
	private val pitchKey:Signal[Option[Boolean]]	= 
			Key(VK_HOME,	KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_END,		KEY_LOCATION_STANDARD).asModifier
	private val pitchButton:Signal[Option[Boolean]]	=
			upButton.pressed	upDown
			downButton.pressed
	private val pitch:Events[Int]	=
			(pitchKey	merge pitchButton).repeated.steps	orElse
			speedEditor.wheel
	pitch.withFine	trigger speed.moveSteps
	
	// setter
	
	speedEditor.changes.withFine	trigger	speed.setValueRastered
	
	// display
	
	speedString observeNow speedDisplay.setText
}
