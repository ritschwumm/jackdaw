package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import java.awt.event._
import javax.swing._

import scutil.implicits._
import scutil.gui.implicits._
import scutil.gui.GridBagDSL._

import screact._

import jackdaw.model._
import jackdaw.gui.util._

import GridBagItem.UI_is_GridBagItem

object ToneUI {
	def spacer	=
			new SpacerUI doto {
				_.component setAllSizes new Dimension(0, 164)
			}
}

/** three knobs for an equalizer */
final class ToneUI(tone:Tone, focusInput:KeyInput) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val trimKnob	= UIFactory trimRotary		tone.trim
	private val filterKnob	= UIFactory filterRotary	tone.filter
	private val highKnob	= UIFactory trimRotary		tone.high
	private val midKnob		= UIFactory trimRotary		tone.middle
	private val lowKnob		= UIFactory trimRotary		tone.low
	
	trimKnob.component		setAllSizes	Style.rotary.size
	filterKnob.component	setAllSizes	Style.rotary.size
	highKnob.component		setAllSizes	Style.rotary.size
	midKnob.component		setAllSizes	Style.rotary.size
	lowKnob.component		setAllSizes	Style.rotary.size
	
	private val panel	=
			GridBagUI(
				trimKnob	pos(0,0) size(1,1) weight(1,1) fill NONE insetsTLBR(0,0,2,0),
				filterKnob	pos(0,1) size(1,1) weight(1,1) fill NONE insetsTLBR(2,0,8+2,0),
				highKnob	pos(0,2) size(1,1) weight(1,1) fill NONE insetsTLBR(2,0,2,0),
				midKnob		pos(0,3) size(1,1) weight(1,1) fill NONE insetsTLBR(2,0,2,0),
				lowKnob		pos(0,4) size(1,1) weight(1,1) fill NONE insetsTLBR(2,0,0,0)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	import KeyEvent._
	import focusInput._
	import ActionUtil._
	
	// actions
	
	private val resetAll:Events[Unit]	=
			Key(VK_INSERT,		KEY_LOCATION_NUMPAD).asAction	orElse
			Key(VK_NUMPAD0,		KEY_LOCATION_NUMPAD).asAction
	resetAll trigger tone.resetAll
	
	private val resetTrim:Events[Unit]	=
			Key(VK_MULTIPLY,		KEY_LOCATION_NUMPAD).asAction
	resetTrim trigger tone.resetTrim
	
	private val resetFilter:Events[Unit]	=
			Key(VK_MULTIPLY,		KEY_LOCATION_NUMPAD).asAction
	resetFilter trigger tone.resetFilter
	
	private val resetLow:Events[Unit]	=
			Key(VK_KP_DOWN,		KEY_LOCATION_NUMPAD).asAction	orElse
			Key(VK_NUMPAD2,		KEY_LOCATION_NUMPAD).asAction
	resetLow trigger tone.resetLow
	
	private val resetMiddle:Events[Unit]	=
			Key(VK_BEGIN,		KEY_LOCATION_NUMPAD).asAction	orElse
			Key(VK_NUMPAD5,		KEY_LOCATION_NUMPAD).asAction
	resetMiddle trigger tone.resetMiddle
			
	private val resetHigh:Events[Unit]	=
			Key(VK_KP_UP,		KEY_LOCATION_NUMPAD).asAction	orElse
			Key(VK_NUMPAD8,		KEY_LOCATION_NUMPAD).asAction
	resetHigh trigger tone.resetHigh
	
	// private val moveTrimKey:Signal[Option[Boolean]]	=
	// 		Key(VK_SUBTRACT,	KEY_LOCATION_NUMPAD).asModifier	upDown
	// 		Key(VK_DIVIDE,		KEY_LOCATION_NUMPAD).asModifier
	private val moveTrim:Events[Int]	=
			// moveTrimKey.repeated.steps	orElse
			trimKnob.wheel
	moveTrim.withFine	trigger tone.moveTrim
	
	private val moveFilterKey:Signal[Option[Boolean]]	=
			Key(VK_SUBTRACT,	KEY_LOCATION_NUMPAD).asModifier	upDown
			Key(VK_DIVIDE,		KEY_LOCATION_NUMPAD).asModifier
	private val moveFilter:Events[Int]	=
			moveFilterKey.repeated.steps	orElse
			filterKnob.wheel
	moveFilter.withFine	trigger tone.moveFilter
	
	private val moveLowKey:Signal[Option[Boolean]]	=
			(	Key(VK_PAGE_DOWN,	KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD3,		KEY_LOCATION_NUMPAD).asModifier
			) upDown
			(	Key(VK_END,			KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD1,		KEY_LOCATION_NUMPAD).asModifier
			)
	private val moveLow:Events[Int]	=
			moveLowKey.repeated.steps	orElse
			lowKnob.wheel
	moveLow.withFine	trigger tone.moveLow
	
	private val moveMiddleKey:Signal[Option[Boolean]]	=
			(	Key(VK_KP_RIGHT,	KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD6,		KEY_LOCATION_NUMPAD).asModifier
			) upDown
			(	Key(VK_KP_LEFT,		KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD4,		KEY_LOCATION_NUMPAD).asModifier
			)
	private val moveMiddle:Events[Int]	=
			moveMiddleKey.repeated.steps	orElse
			midKnob.wheel
	moveMiddle.withFine	trigger tone.moveMiddle
					
	private val moveHighKey:Signal[Option[Boolean]]	=
			(	Key(VK_PAGE_UP,		KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD9,		KEY_LOCATION_NUMPAD).asModifier
			) upDown
			(	Key(VK_HOME,		KEY_LOCATION_NUMPAD).asModifier	orElse
				Key(VK_NUMPAD7,		KEY_LOCATION_NUMPAD).asModifier
			)
	private val moveHigh:Events[Int]	=
			moveHighKey.repeated.steps	orElse
			highKnob.wheel
	moveHigh.withFine	trigger tone.moveHigh
	
	// setter
	
	trimKnob.changes	observe tone.trim.set
	filterKnob.changes	observe tone.filter.set
	highKnob.changes	observe tone.high.set
	midKnob.changes		observe tone.middle.set
	lowKnob.changes		observe tone.low.set
}
