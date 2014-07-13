package djane.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.GridBagDSL._

import screact._

import djane.model._
import djane.gui.action._

import GridBagItem.UI_is_GridBagItem

/** mastering volumes and meter */
final class ChannelUI(strip:Strip, tone:Option[Tone], peak:Signal[Float], phoneEnabled:Boolean, keyboardEnabled:Signal[Boolean]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## components
	
	private val topUI	= 
			new DelayUI(
					tone cata (
						ToneUI.spacer,
						tone => new ToneUI(tone, focusInput)
					))
	
	private val stripUI	= 
			new DelayUI(new StripUI(strip, peak, phoneEnabled, focusInput))
	
	private val z		= (Style.linear.knob.size / 2).toInt
	private val	panel	= 
			GridBagUI(
				topUI		pos(0,0) size(1,1) weight(1,0) fill NONE		anchor CENTER	insetsTLBR(0,0,6,0),
				stripUI		pos(0,1) size(1,1) weight(1,1) fill VERTICAL	anchor EAST		insetsTLBR(6,0,0,0)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	private val focusInput	= 
			KeyInput focusInput (
				enabled		= keyboardEnabled,
				component	= component,
				off			= Style.strip.border.noFocus,
				on			= Style.strip.border.inFocus
			)

	// TODO ugly
	topUI.init()
	stripUI.init()
}
