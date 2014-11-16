package jackdaw.gui

// import java.awt.{ List=>AwtList, _ }
import java.awt.event._
import javax.swing._

import scutil.gui.implicits._
import scutil.gui.GridBagDSL._

import screact._

import jackdaw.model._
import jackdaw.gui.action._
import jackdaw.gui.util._

import GridBagItem.UI_is_GridBagItem

final class StripUI(strip:Strip, peak:Signal[Float], phoneEnabled:Boolean, focusInput:KeyInput) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val speakerUI	= UIFactory volumeLinear	strip.speaker
	private val phoneUI		= UIFactory volumeRotary	strip.phone
	private val meterUI		= UIFactory	meter			(peak, strip.meterRange)
	
	speakerUI.component	setAllSizes	Style.linear.size
	phoneUI.component	setAllSizes	Style.rotary.size
	meterUI.component 	setAllSizes	Style.meter.size
	
	phoneUI.component setVisible phoneEnabled
	
	private val z		= (Style.linear.knob.size / 2).toInt
	private val	panel	=
			GridBagUI(
				speakerUI	pos(0,1) size(1,1) weight(1,1) fill VERTICAL	anchor EAST		insetsTLBR(0,0,2,2),
				meterUI		pos(1,1) size(1,1) weight(1,1) fill VERTICAL	anchor WEST		insetsTLBR(0+z,2,2+z,0),
				phoneUI		pos(0,2) size(2,1) weight(1,0) fill NONE		anchor CENTER	insetsTLBR(2,0,0,0)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	import KeyEvent._
	import focusInput._
	import ActionUtil._
	
	// actions
	
	private val moveSpeakerKey:Signal[Option[Boolean]]	= 
			Key(VK_ADD,		KEY_LOCATION_NUMPAD).asModifier	upDown
			Key(VK_ENTER,	KEY_LOCATION_NUMPAD).asModifier
	private val moveSpeaker:Events[Int]	= 
			moveSpeakerKey.repeated.steps	orElse
			speakerUI.wheel
	moveSpeaker.withFine	trigger strip.moveSpeaker
	
	// private val movePhoneKey:Signal[Option[Boolean]]	= 
	// 		Key(VK_MULTIPLY,	KEY_LOCATION_NUMPAD).asModifier	upDown
	// 		Key(VK_DIVIDE,		KEY_LOCATION_NUMPAD).asModifier
	private val movePhone:Events[Int]	= 
			// movePhoneKey.repeated.steps	orElse
			phoneUI.wheel
	movePhone.withFine	trigger strip.movePhone

	// setter
	
	speakerUI.changes	observe	strip.speaker.set
	phoneUI.changes		observe	strip.phone.set
}
