package jackdaw.gui

import javax.swing._

import scutil.gui.GridBagDSL._
	
import screact._

import jackdaw.model._

import GridBagItem.UI_is_GridBagItem

/** metadata for a track */
final class MetaUI(deck:Deck) extends UI {
	//------------------------------------------------------------------------------
	//## input
	
	private val title		= signal { deck.metadata.current flatMap { _.title 	} orElse deck.fileName.current getOrElse "" }
	private val artist		= signal { deck.metadata.current flatMap { _.artist	} getOrElse "" }
	
	private val location	= deck.playerRhythmIndex	map Render.rhythmIndexOpt
	private val remains		= deck.playerBeforeCuePoint	map Render.beatsOpt
	/*
	private val remains		= deck.playerRemainingSeconds	map Render.secondsOpt
	*/
	
	private val	bpm			= deck.beatRate			map Render.bpmOpt
	private val	pitch		= deck.pitchOctave		map Render.cents
	
	//------------------------------------------------------------------------------
	//## components
	
	private val titleDisplay		= new MetaDisplayUI(title,		strong	= true,		start	= true)
	private val artistDisplay		= new MetaDisplayUI(artist,		strong	= false,	start	= true)
	
	private val locationDisplay		= new MetaDisplayUI(location,	strong	= true,		start	= false)
	private val remainsDisplay		= new MetaDisplayUI(remains,	strong	= false,	start	= false)
	
	private val bpmDisplay			= new MetaDisplayUI(bpm,		strong	= true,		start	= false)
	private val pitchDisplay		= new MetaDisplayUI(pitch,		strong	= false,	start	= false)
	
	private val annotationEditor	= new MetaEditUI(deck.annotation,	strong	= false)

	private val panel	= GridBagUI(
		titleDisplay		pos(0,0) size(1,1) weight(1,1) fill HORIZONTAL insetsTLBR(2,4,0,4),
		artistDisplay		pos(0,1) size(1,1) weight(1,1) fill HORIZONTAL insetsTLBR(0,4,0,4),
		locationDisplay 	pos(1,0) size(1,1) weight(0,1) fill HORIZONTAL insetsTLBR(2,4,0,4),
		remainsDisplay		pos(1,1) size(1,1) weight(0,1) fill HORIZONTAL insetsTLBR(0,4,0,4),
		bpmDisplay			pos(2,0) size(1,1) weight(0,1) fill HORIZONTAL insetsTLBR(2,4,0,4),
		pitchDisplay		pos(2,1) size(1,1) weight(0,1) fill HORIZONTAL insetsTLBR(0,4,0,4),
		annotationEditor	pos(0,2) size(3,1) weight(1,1) fill HORIZONTAL insetsTLBR(0,4,2,4)
	)
	panel.component setBackground	Style.meta.background.color
	panel.component setBorder		Style.meta.border
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## actions
	
	def editAnnotation() {
		annotationEditor.startEditing()
	}
	
	//------------------------------------------------------------------------------
	//## output
	
	val grabsKeyboard:Signal[Boolean]	= annotationEditor.focussed
	val onAnnotation:Events[String]		= annotationEditor.changes
}
