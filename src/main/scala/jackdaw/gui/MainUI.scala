package jackdaw.gui

import javax.swing._

import scutil.gui.GridBagDSL._
import scutil.gui.GlobalKeyEvent
import scutil.gui.GlobalAWTEvent

import screact._

import jackdaw.model._

import GridBagItem.UI_is_GridBagItem

/** the complete application window */
final class MainUI(model:Model, windowActive:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## feedback
	
	private val keyboardEnabled	= cell(true)
	
	//------------------------------------------------------------------------------
	//## components
	
	private val deck1UI		= new DeckUI(model.deck1, keyboardEnabled)
	private val deck2UI		= new DeckUI(model.deck2, keyboardEnabled)
	private val deck3UI		= new DeckUI(model.deck3, keyboardEnabled)
	
	private val deckPanel	=
			GridBagUI(
				deck1UI	pos(0,0) size(1,1) weight(1,1)	fill BOTH insetsTLBR(0,0,6,0),
				deck2UI	pos(0,1) size(1,1) weight(1,1)	fill BOTH insetsTLBR(6,0,6,0),
				deck3UI	pos(0,2) size(1,1) weight(1,1)	fill BOTH insetsTLBR(6,0,0,0)
			)
	
	private val channel1UI	= new ChannelUI(model.mix.strip1, Some(model.mix.tone1),	model.masterPeak1,	model.phoneEnabled, keyboardEnabled)
	private val channel2UI	= new ChannelUI(model.mix.strip2, Some(model.mix.tone2),	model.masterPeak2,	model.phoneEnabled, keyboardEnabled)
	private val channel3UI	= new ChannelUI(model.mix.strip3, Some(model.mix.tone3),	model.masterPeak3,	model.phoneEnabled, keyboardEnabled)
	private val masterUI	= new ChannelUI(model.mix.master, None,						model.masterPeak,	model.phoneEnabled, keyboardEnabled)
	private val speedUI		= new SpeedUI(model.speed, keyboardEnabled)
	
	private val	masterPanel	= 
			GridBagUI(
				channel1UI	pos(0,0) size(1,1) weight(1,1)	fill VERTICAL	anchor NORTH 	insetsTLBR(0,0,12,6),
				channel2UI	pos(1,0) size(1,1) weight(1,1)	fill VERTICAL	anchor NORTH	insetsTLBR(0,6,12,6),
				channel3UI	pos(2,0) size(1,1) weight(1,1)	fill VERTICAL	anchor NORTH	insetsTLBR(0,6,12,10),
				masterUI	pos(3,0) size(1,1) weight(1,1)	fill VERTICAL	anchor NORTH	insetsTLBR(0,10,12,0),
				speedUI		pos(0,1) size(4,1) weight(1,0)	fill BOTH		anchor CENTER	insetsTLBR(12,0,0,0)
			)
	
	private val panel	=
			GridBagUI(
				deckPanel	pos(0,0) size(1,1) weight(1,1)	fill BOTH insetsTLBR(8,0,6,14),
				masterPanel	pos(1,0) size(0,1) weight(0,1)	fill BOTH insetsTLBR(0,14,0,12)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	private val grabsKeyboard	=
			signal {
				deck1UI.grabsKeyboard.current	||
				deck2UI.grabsKeyboard.current	||
				deck3UI.grabsKeyboard.current
			}
			
	private val keyboardEnabledFb	= 
			(windowActive zipWith grabsKeyboard) {
				_ && !_
			}
	
	keyboardEnabledFb observeNow keyboardEnabled.set
}
