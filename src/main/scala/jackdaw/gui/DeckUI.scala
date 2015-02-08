package jackdaw.gui

import java.io.File
import java.awt.event._
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.implicits._
import scutil.gui.DndFileImport
import scutil.gui.DndFileExport
import scutil.gui.GridBagDSL._
import scutil.log._

import screact._

import jackdaw.data._
import jackdaw.model._
import jackdaw.gui.action._
import jackdaw.gui.util._

import GridBagItem.UI_is_GridBagItem

/** gui for a single deck */
final class DeckUI(deck:Deck, keyboardEnabled:Signal[Boolean]) extends UI with Observing with Logging {
	//------------------------------------------------------------------------------
	//## input
	
	private val rhythmLines:Signal[ISeq[RhythmLine]]	=
			deck.rhythmLines map { _.flattenMany }
		
	private val cuePoints:Signal[ISeq[Double]]	=
			deck.cuePointsFlat
	
	private val cuePointsCount:Signal[Int]	= 
			cuePoints map { _.size }
		
	private val loopChoices:ISeq[(LoopDef,Signal[Boolean])]	=
			LoopDef.all map { it =>
				val active	= deck.loopDef map { _ ==== (Some(it):Option[LoopDef]) }
				(it, active)
			}
		
	private val rhythmic:Signal[Boolean]	=
			deck.rhythm map { _.isDefined }
	
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
				rhythmAnchor	= deck.rhythmAnchor,
				loop			= deck.loopSpan,
				widthOrigin		= 0.5,
				shrink			= false
			)
	private val overviewUI	=
			new WaveUI(
				bandCurve		= deck.bandCurve,
				frameOrigin		= static(0.0),
				playerPosition	= deck.position,
				cuePoints		= cuePoints,
				rhythmLines		= static(ISeq.empty),
				rhythmAnchor	= deck.rhythmAnchor,
				loop			= deck.loopSpan,
				widthOrigin		= 0.0,
				shrink			= true
			)
	private val phaseUI			= new PhaseUI(deck.measureMatch, deck.rhythm)
	private val transportUI		=
			new TransportUI(
				cueable			= deck.dataLoaded,
				playable		= deck.sampleLoaded,
				playing			= deck.running,
				afterEnd		= deck.afterEnd,
				loopChoices		= loopChoices,
				rhythmic		= rhythmic,
				cuePointsCount	= cuePointsCount
			)
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
	
	private val phaseSyncKey:Events[Double]	=
			Key(VK_MINUS ,	KEY_LOCATION_STANDARD).asAction tag 0.0
	private val phaseSync:Events[Double]	=
			phaseSyncKey	orElse
			phaseUI.jump
	phaseSync observe { phase =>
		deck syncPhase (Measure, phase)
	}
		
	// TODO crude hack
	private val pushing16Key:Events[Double]	=
			Key(VK_COMMA,	KEY_LOCATION_STANDARD).asModifier
			.edge map { _ cata (-1.0/4, +1.0/4) }
	private val dragging8Key:Events[Double]	=
			Key(VK_PERIOD,	KEY_LOCATION_STANDARD).asModifier
			.edge map { _ cata (+1.0/2, -1.0/2) }
	private val phasingKey:Events[Double]	=
			pushing16Key orElse
			dragging8Key
	phasingKey observe { fraction =>
		deck modifyPhase (Beat, fraction)
	}
	
	phaseUI.mouseWheel.withFine	trigger	{ (steps, fine)	=>
		deck movePhase	(Measure, steps, fine)	
	}

	private val ejectTrackKey:Events[Unit]	=
			Key(VK_LESS ,	KEY_LOCATION_STANDARD).asAction
	private val ejectTrack:Events[Unit]	=
			ejectTrackKey		orElse
			transportUI.eject
	ejectTrack trigger deck.ejectTrack
		
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
	
	private val setLoopKey:Events[Option[LoopDef]]	=
			Events multiOrElse (
				(loopChoices zip ISeq(VK_U, VK_I, VK_O, VK_P))
				.map { case ((loopDef, active), key) =>
					Key(key, KEY_LOCATION_STANDARD).asAction tag Some(loopDef)
				}
			)
	private val resetLoopKey:Events[Option[LoopDef]]	=
			Key(VK_NUMBER_SIGN,	KEY_LOCATION_STANDARD).asAction tag None
	private val setLoop:Events[Option[LoopDef]]	=
			setLoopKey		orElse
			resetLoopKey	orElse
			transportUI.setLoop
	setLoop observe	deck.setLoop
	
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
			Events multiOrElse (
				Vector(
					VK_0			-> 0,
					VK_1			-> 1,
					VK_2			-> 2,
					VK_3			-> 3,
					VK_4			-> 4,
					VK_5			-> 5,
					VK_6			-> 6,
					VK_7			-> 7,
					VK_8			-> 8,
					VK_9			-> 9,
					VK_CIRCUMFLEX	-> 0,
					VK_BACK_QUOTE	-> 0
				)
				.map { case (k, i) =>
					(Key(k,	KEY_LOCATION_STANDARD).asAction tag i)
				}
			)
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
	
	DndFileExport install (
		component,
		_ => deck.track.current map { it => Nes single it.file }
	)
}
