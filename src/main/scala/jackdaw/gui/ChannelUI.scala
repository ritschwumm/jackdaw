package jackdaw.gui

import javax.swing._

import scutil.base.implicits._
import scutil.gui.GridBagDSL._

import screact._

import jackdaw.model._
import jackdaw.gui.util._

import GridBagItem.UI_is_GridBagItem

/** mastering volumes and meter */
final class ChannelUI(strip:Strip, tone:Option[Tone], peak:Signal[Float], phoneEnabled:Boolean, keyTarget:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val delayedToneUI	=
			new DelayUI(
					tone cata (
						ToneUI.spacer,
						tone => new ToneUI(tone, keyInput)
					)
				)
	
	private val delayedStripUI	=
			new DelayUI(new StripUI(strip, peak, phoneEnabled, keyInput))
	
	//private val z		= (Style.linear.knob.size / 2).toInt
	private val	panel	=
			GridBagUI(
				delayedToneUI	pos(0,0) size(1,1) weight(1,0) fill NONE		anchor CENTER	insetsTLBR(0,0,6,0),
				delayedStripUI	pos(0,1) size(1,1) weight(1,1) fill VERTICAL	anchor EAST		insetsTLBR(6,0,0,0)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	private val border	= keyTarget map { _ cata (Style.channel.border.noFocus, Style.channel.border.inFocus) }
	border observeNow component.setBorder
	
	val hovered	= ComponentUtil underMouseSignal component
	
	// // NOTE forward reference works because of the DelayUI
	private val keyInput	= KeyInput when keyTarget

	delayedToneUI.init()
	delayedStripUI.init()
}
