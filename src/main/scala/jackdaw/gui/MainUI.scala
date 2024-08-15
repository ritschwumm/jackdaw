package jackdaw.gui

import javax.swing.*

import screact.*

import jackdaw.model.*
import jackdaw.gui.util.*

/** the complete application window */
final class MainUI(model:Model, keyboard:Signal[Set[Key]], windowActive:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## feedback

	private val grabsKeyboardFb	= cell(false)

	private val keyboardEnabled:Signal[Boolean]	=
		(windowActive `map2` grabsKeyboardFb.signal)(_ && !_)

	private val deck1HoveredFb		= cell(false)
	private val deck2HoveredFb		= cell(false)
	private val deck3HoveredFb		= cell(false)
	private val channel1HoveredFb	= cell(false)
	private val channel2HoveredFb	= cell(false)
	private val channel3HoveredFb	= cell(false)
	private val masterHoveredFb		= cell(false)
	private val speedHoveredFb		= cell(false)

	private def target(hovered:Signal[Boolean]):Signal[Boolean]	=
		(keyboardEnabled `map2` hovered)(_ && _)

	private def or(a:Signal[Boolean], b:Signal[Boolean]):Signal[Boolean]	=
		(a `map2` b)(_ || _)

	private val deck1Target		= target(or(deck1HoveredFb.signal, channel1HoveredFb.signal))
	private val deck2Target		= target(or(deck2HoveredFb.signal, channel2HoveredFb.signal))
	private val deck3Target		= target(or(deck3HoveredFb.signal, channel3HoveredFb.signal))
	private val channel1Target	= deck1Target
	private val channel2Target	= deck2Target
	private val channel3Target	= deck3Target
	private val masterTarget	= target(masterHoveredFb.signal)
	private val speedTarget		= target(speedHoveredFb.signal)
	/*
	private val deck1Target		= target(deck1HoveredFb)
	private val deck2Target		= target(deck2HoveredFb)
	private val deck3Target		= target(deck3HoveredFb)
	private val channel1Target	= target(channel1HoveredFb)
	private val channel2Target	= target(channel2HoveredFb)
	private val channel3Target	= target(channel3HoveredFb)
	private val masterTarget	= target(masterHoveredFb)
	private val speedTarget		= target(speedHoveredFb)
	*/

	//------------------------------------------------------------------------------
	//## components

	private val deck1UI		= new DeckUI(model.deck1, keyboard, deck1Target)
	private val deck2UI		= new DeckUI(model.deck2, keyboard, deck2Target)
	private val deck3UI		= new DeckUI(model.deck3, keyboard, deck3Target)

	private val deckPanel	=
		GridBagUI(
			deck1UI	.gbi.pos(0,0) .size(1,1) .weight(1,1)	.fill("BOTH") .insetsTLBR(0,0,6,0),
			deck2UI	.gbi.pos(0,1) .size(1,1) .weight(1,1)	.fill("BOTH") .insetsTLBR(6,0,6,0),
			deck3UI	.gbi.pos(0,2) .size(1,1) .weight(1,1)	.fill("BOTH") .insetsTLBR(6,0,0,0)
		)

	private val channel1UI	= new ChannelUI(model.mix.strip1, Some(model.mix.tone1),	model.masterPeak1,	model.phoneEnabled, keyboard, channel1Target)
	private val channel2UI	= new ChannelUI(model.mix.strip2, Some(model.mix.tone2),	model.masterPeak2,	model.phoneEnabled, keyboard, channel2Target)
	private val channel3UI	= new ChannelUI(model.mix.strip3, Some(model.mix.tone3),	model.masterPeak3,	model.phoneEnabled, keyboard, channel3Target)
	private val masterUI	= new ChannelUI(model.mix.master, None,						model.masterPeak,	model.phoneEnabled, keyboard, masterTarget)
	private val speedUI		= new SpeedUI(model.speed, keyboard, speedTarget)

	private val	masterPanel	=
		GridBagUI(
			channel1UI	.gbi.pos(0,0) .size(1,1) .weight(1,1)	.fill("VERTICAL")	.anchor("NORTH")	.insetsTLBR(0,0,12,6),
			channel2UI	.gbi.pos(1,0) .size(1,1) .weight(1,1)	.fill("VERTICAL")	.anchor("NORTH")	.insetsTLBR(0,6,12,6),
			channel3UI	.gbi.pos(2,0) .size(1,1) .weight(1,1)	.fill("VERTICAL")	.anchor("NORTH")	.insetsTLBR(0,6,12,10),
			masterUI	.gbi.pos(3,0) .size(1,1) .weight(1,1)	.fill("VERTICAL")	.anchor("NORTH")	.insetsTLBR(0,10,12,0),
			speedUI		.gbi.pos(0,1) .size(4,1) .weight(1,0)	.fill("BOTH")		.anchor("CENTER")	.insetsTLBR(12,0,0,0)
		)

	private val panel	=
		GridBagUI(
			deckPanel	.gbi.pos(0,0) .size(1,1) .weight(1,1)	.fill("BOTH") .insetsTLBR(8,0,6,14),
			masterPanel	.gbi.pos(1,0) .size(0,1) .weight(0,1)	.fill("BOTH") .insetsTLBR(0,14,0,12)
		)
	val component:JComponent	= panel.component
	component.putClientProperty("STRONG_REF", this)

	//------------------------------------------------------------------------------
	//## wiring

	deck1UI.hovered		.observeNow(deck1HoveredFb.set)
	deck2UI.hovered		.observeNow(deck2HoveredFb.set)
	deck3UI.hovered		.observeNow(deck3HoveredFb.set)
	channel1UI.hovered	.observeNow(channel1HoveredFb.set)
	channel2UI.hovered	.observeNow(channel2HoveredFb.set)
	channel3UI.hovered	.observeNow(channel3HoveredFb.set)
	masterUI.hovered	.observeNow(masterHoveredFb.set)
	speedUI.hovered		.observeNow(speedHoveredFb.set)

	private val grabsKeyboard	=
		signal {
			deck1UI.grabsKeyboard.current	||
			deck2UI.grabsKeyboard.current	||
			deck3UI.grabsKeyboard.current
		}
	grabsKeyboard.observeNow(grabsKeyboardFb.set)
}
