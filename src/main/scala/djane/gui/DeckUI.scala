package djane.gui

import java.io.File
// import java.awt.{ List=>AwtList, _ }
import java.awt.event._
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.implicits._
import scutil.gui.DndFileImport
import scutil.gui.GridBagDSL._
import scutil.log._

import screact._

import djane.model._
import djane.gui.action._
import djane.gui.util._

import GridBagItem.UI_is_GridBagItem

/** gui for a single deck */
final class DeckUI(deck:Deck, keyboardEnabled:Signal[Boolean]) extends UI with Observing with Logging {
	//------------------------------------------------------------------------------
	//## input
	
	private val rhythmLines:Signal[ISeq[RhythmLine]]	= 
			deck.rhythmLines map { _.toISeq.flatten }
		
	private val anchorLines:Signal[ISeq[RhythmLine]]	=
			rhythmLines map { _ collect { case x@RhythmLine.AnchorLine(_) => x } }
	
	private val cuePoints:Signal[ISeq[Double]]	=
			deck.cuePointsFlat
	
	private val cuePointsCount	= 
			cuePoints map { _.size }
	
	//------------------------------------------------------------------------------
	//## components
	
	private val metaUI		= new MetaUI(deck)
	private val detailUI	=
			new WaveUI(
				bandCurve		= deck.bandCurve,
				frameOrigin		= deck.position, 
				playerPosition	= deck.position,
				cuePoints		= cuePoints,
				rhythmLines		= rhythmLines,
				widthOrigin		= 0.5,
				shrink			= false
			)
	private val overviewUI	=
			new WaveUI(
				bandCurve		= deck.bandCurve,
				frameOrigin		= static(0.0),
				playerPosition	= deck.position,
				cuePoints		= cuePoints,
				rhythmLines		= anchorLines,
				widthOrigin		= 0.0,
				shrink			= true
			)
	private val phaseUI			= new PhaseUI(deck.measureMatch, deck.rhythm)
	private val transportUI		= new TransportUI(deck.running, deck.afterEnd, deck.loaded, cuePointsCount)
	private val pitchSlider		= UIFactory pitchLinear deck.pitchOctave
	private val matchUI			= new MatchUI(deck.synced, deck.pitched)
	
	phaseUI.component		setAllSizes	Style.phase.size
	pitchSlider.component	setAllSizes	Style.linear.size
	
	private val panel	=
			GridBagUI(
				phaseUI		pos(0,0) size(1,1) weight(1,0)		fill BOTH				insetsTLBR(0,0,2,2),
				metaUI		pos(0,1) size(1,1) weight(1,0)		fill BOTH				insetsTLBR(2,0,2,2),
				detailUI	pos(0,2) size(1,1) weight(1,1)		fill BOTH				insetsTLBR(2,0,2,2),
				overviewUI	pos(0,3) size(1,1) weight(1,.33)	fill BOTH				insetsTLBR(2,0,2,2),
				transportUI	pos(0,4) size(1,1) weight(1,0)		fill BOTH				insetsTLBR(2,2,0,2),
				pitchSlider	pos(1,0) size(1,4) weight(0,1)		fill BOTH				insetsTLBR(0,2,2,2),
				matchUI		pos(2,0) size(1,4) weight(0,1)		fill NONE anchor CENTER	insetsTLBR(2,2,6,0)
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## wiring
	
	val grabsKeyboard	= metaUI.grabsKeyboard
	
	import KeyEvent._
	
	private val focusInput	= 
			KeyInput focusInput (
				enabled		= keyboardEnabled,
				component	= component,
				off			= Style.deck.border.noFocus,
				on			= Style.deck.border.inFocus
			)
	import focusInput._
	
	import ActionUtil._
	
	// modifiers
		
	private val draggingKey:Signal[Option[Boolean]]	=
			Key(VK_UP,		KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_DOWN,	KEY_LOCATION_STANDARD).asModifier
	private val dragging:Signal[Option[Boolean]]	=
			draggingKey	merge
			matchUI.dragging
	dragging.withFine	observeNow	deck.dragging.set
	
	// actions
	
	overviewUI.jump.withFine	trigger	deck.jumpFrame
	
	phaseUI.jump				observe	(deck syncPhaseManually	(RhythmUnit.Measure, _))
	phaseUI.mouseWheel.withFine	trigger	(deck movePhase			(RhythmUnit.Measure, _:Int, _:Boolean))

	transportUI.eject trigger deck.ejectTrack
		
	private val editAnnotationKey:Events[Unit]	=
			Key(VK_E,		KEY_LOCATION_STANDARD).asAction
	editAnnotationKey trigger metaUI.editAnnotation
	
	private val playToggleKey:Events[Unit]	=
			Key(VK_SPACE,		KEY_LOCATION_STANDARD).asAction
	private val playToggle:Events[Unit]	=
			playToggleKey		orElse
			detailUI.playToggle	orElse
			transportUI.playToggle	
	playToggle	trigger	deck.playToggle
	
	private val syncToggleKey:Events[Unit]	=
			Key(VK_INSERT,		KEY_LOCATION_STANDARD).asAction
	private val syncToggle:Events[Unit]	=
			syncToggleKey	orElse
			matchUI.syncToggle
	syncToggle trigger deck.syncToggle
	
	private val resetPitchKey:Events[Unit]	=
			Key(VK_DELETE,		KEY_LOCATION_STANDARD).asAction
	private val resetPitch:Events[Unit]	=
			resetPitchKey	orElse
			matchUI.reset
	resetPitch	trigger deck.resetPitch
	
	private val changePitchKey:Signal[Option[Boolean]]	=
			Key(VK_HOME,	KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_END,		KEY_LOCATION_STANDARD).asModifier
	private val changePitch:Events[Int]	=
			(changePitchKey merge matchUI.pitch).repeated.steps	orElse
			pitchSlider.wheel
	changePitch.withFine	trigger	deck.changePitch
	
	private val changeRhythmAnchor:Events[Unit]	=
			Key(VK_T,			KEY_LOCATION_STANDARD).asAction
	changeRhythmAnchor trigger deck.changeRhythmAnchor
	
	private val toggleRhythm:Events[Unit]	=
			Key(VK_R,			KEY_LOCATION_STANDARD).asAction
	toggleRhythm trigger deck.toggleRhythm
	
	private val seekKey:Signal[Option[Boolean]]	=
			Key(VK_RIGHT,	KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_LEFT,	KEY_LOCATION_STANDARD).asModifier
	private val seek:Events[Int]	=
			(seekKey merge transportUI.seeking).repeated.steps	orElse
			detailUI.seek	orElse
			overviewUI.seek
	seek.withFine	trigger	deck.seek
	
	private val resizeRhythmAt:Signal[Option[Boolean]]	=
			Key(VK_W,	KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_Q,	KEY_LOCATION_STANDARD).asModifier
	resizeRhythmAt.repeated.withFine	trigger	deck.resizeRhythmAt
	
	private val resizeRhythmBy:Signal[Option[Boolean]]	=
			Key(VK_S,	KEY_LOCATION_STANDARD).asModifier	upDown
			Key(VK_A,	KEY_LOCATION_STANDARD).asModifier
	resizeRhythmBy.repeated.withFine	trigger	deck.resizeRhythmBy
	
	private val moveRhythmBy:Signal[Option[Boolean]]	=
			Key(VK_X,	KEY_LOCATION_STANDARD).asModifier	upDown (
				Key(VK_Y,	KEY_LOCATION_STANDARD).asModifier orElse
				// for US keyboards
				Key(VK_Z,	KEY_LOCATION_STANDARD).asModifier
			)
	moveRhythmBy.repeated.withFine	trigger	deck.moveRhythmBy
	
	private val addCueKey:Events[Unit]	=
			Key(VK_ENTER,		KEY_LOCATION_STANDARD).asAction
	private val addCue:Events[Unit]	=
			addCueKey	orElse
			transportUI.addCue
	addCue.withFine	observe deck.addCue
	
	private val removeCueKey:Events[Unit]	=
			Key(VK_BACK_SPACE,	KEY_LOCATION_STANDARD).asAction
	private val removeCue:Events[Unit]	=
			removeCueKey	orElse
			transportUI.removeCue
	removeCue	trigger deck.removeCue
	
	private val jumpCueKey:Events[Int]	=
			(Key(VK_0,			KEY_LOCATION_STANDARD).asAction tag 0)	orElse
			(Key(VK_1,			KEY_LOCATION_STANDARD).asAction tag 1)	orElse
			(Key(VK_2,			KEY_LOCATION_STANDARD).asAction tag 2)	orElse
			(Key(VK_3,			KEY_LOCATION_STANDARD).asAction tag 3)	orElse
			(Key(VK_4,			KEY_LOCATION_STANDARD).asAction tag 4)	orElse
			(Key(VK_5,			KEY_LOCATION_STANDARD).asAction tag 5)	orElse
			(Key(VK_6,			KEY_LOCATION_STANDARD).asAction tag 6)	orElse
			(Key(VK_7,			KEY_LOCATION_STANDARD).asAction tag 7)	orElse
			(Key(VK_8,			KEY_LOCATION_STANDARD).asAction tag 8)	orElse
			(Key(VK_9,			KEY_LOCATION_STANDARD).asAction tag 9)	orElse
			(Key(VK_CIRCUMFLEX,	KEY_LOCATION_STANDARD).asAction tag 0)	orElse
			(Key(VK_BACK_QUOTE,	KEY_LOCATION_STANDARD).asAction tag 0)
	private val jumpCue:Events[Int]	=
			jumpCueKey	orElse
			transportUI.jumpCue	
	jumpCue.withFine	trigger deck.jumpCue
	
	// setters
	
	metaUI.onAnnotation		observe	deck.setAnnotation
	detailUI.scratchFrame	observe	deck.scratching.set
	pitchSlider.changes		observe	deck.setPitchOctave
	
	// dnd 
	
	// NOTE this has to be done _before_ adding the DropTargetListener or the gui doesn't start any more. why?
	DndFileImport install (
		component,
		_	=> Some {
			(files:Validated[Exception,Nes[File]]) => {
				files
				.badEffect	{ es => ERROR((es.toISeq):_*)	}
				.toOption
				.map		{ _.head }
				.foreach	(deck.loadTrack)
			}
		}
	)
}
