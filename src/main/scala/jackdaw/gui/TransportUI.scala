package jackdaw.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.CasterInstances._

import screact._

import jackdaw.model.LoopDef
import jackdaw.gui.action._

object TransportUI {
	private val maxCuePoints	= ButtonStyleFactory.digitCount
}

/** playback control */
final class TransportUI(trackLoaded:Signal[Boolean], playing:Signal[Boolean], afterEnd:Signal[Boolean], loopChoices:ISeq[(LoopDef,Signal[Boolean])], rhythmic:Signal[Boolean], cuePointsCount:Signal[Int]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## cue point components
	
	private val (cuePointUIs, cuePointActionEvents)	= 
			(cuePointsCount map { count =>
				decouple {
					val (uis, actions)	= 
							(0 until count map { index =>
								val image	= ButtonStyleFactory CUE index
								val button	=
										new ButtonUI(
											ButtonStyleFactory.size,
											static(image),
											trackLoaded
										)
								val actions	= button.actions tag index
								(button, actions)
							})
							.unzip
					(uis, actions)
				}
			})
			.unzip
	// cuePointUIs:Signal[ISeq[UI]]
	// cuePointActionEvents:Signal[ISeq[Events[Int]]]
			
	private val cuePointActions:Events[Int]	= 
			(cuePointActionEvents map Events.multiOrElse).flattenEvents
		
	private val cuePointsPanel:UI	=
			new SwitchUI(
				cuePointUIs map { items =>
					new HBoxUI(items map BoxComponent.apply)
				}
			)
	
	private val canPlay:Signal[Boolean]	= 
			signal {
				!afterEnd.current &&
				trackLoaded.current
			}
			
	private val canLoop:Signal[Boolean]	= 
			signal {
				canPlay.current &&
				rhythmic.current
			}
	
	private val canAddCuePoint:Signal[Boolean]	= 
			signal {
				cuePointsCount.current < TransportUI.maxCuePoints &&
				trackLoaded.current
			}
	
	private val canRemoveCuePoint:Signal[Boolean]	=
			signal {
				cuePointsCount.current != 0 &&
				trackLoaded.current
			}
			
	//------------------------------------------------------------------------------
	//## loop components
	
	val (loopSetUIs, loopSetActionEvents)	=
			(	loopChoices map { case (choice, active) =>
					val style	=
							active map {
								_ cata (ButtonStyleFactory.LOOP_ON _, ButtonStyleFactory.LOOP_OFF _) apply choice.measures
							}
					val button	=
							new ButtonUI(
								size	= ButtonStyleFactory.size,
								style	= style,
								enabled	= canLoop
							)
					val action	=
							button.actions tag Some(choice)
					(button, action)
				}
			).unzip
	val loopResetUI	= 
			new ButtonUI(
				size	= ButtonStyleFactory.size,
				style	= static(ButtonStyleFactory.LOOP_RESET),
				enabled	= canLoop
			)
	private val loopUIs:ISeq[UI]	=
			loopSetUIs :+ loopResetUI
	private val loopActions:Events[Option[LoopDef]]	= 
			(Events multiOrElse loopSetActionEvents)	orElse
			(loopResetUI.actions tag None)
			
	private val loopPanel:UI	=
			new HBoxUI(loopUIs map BoxComponent.apply intersperse BoxStrut(2))
	
	//------------------------------------------------------------------------------
	//## playing components
	
	private val playingIcon:Signal[ButtonStyle]	= 
			playing map { _ cata (ButtonStyleFactory.PLAY, ButtonStyleFactory.PAUSE) }
		
	private val	playToggleButton	= new ButtonUI(ButtonStyleFactory.size, playingIcon,						canPlay)
	
	//------------------------------------------------------------------------------
	//## other components
	
	private val	seekBackwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.LEFT),	trackLoaded)
	private val	seekForwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RIGHT),	trackLoaded)
	private val	ejectButton			= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.EJECT),	trackLoaded)
	private val	addCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RECORD),	canAddCuePoint)
	private val	removeCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.CROSS),	canRemoveCuePoint)
	
	// private val	cueStopButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.STOP),	trackLoaded)
	
	private val panel	= 
			HBoxUI(
				// cueStopButton,
				playToggleButton,
				BoxStrut(4-2),
				ejectButton,
				BoxStrut(4),
				seekBackwardButton,
				BoxStrut(4),
				seekForwardButton,
				BoxStrut(4),
				ejectButton,
				BoxStrut(12),
				loopPanel,
				BoxStrut(12),
				BoxGlue,
				cuePointsPanel,
				BoxStrut(4+2),
				removeCueButton,
				BoxStrut(2),
				addCueButton
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## output
	
	import ActionUtil._
	
	val playToggle:Events[Unit]			= playToggleButton.actions
	val setLoop:Events[Option[LoopDef]]	= loopActions
	val eject:Events[Unit]				= ejectButton.actions
	val jumpCue:Events[Int]				= cuePointActions
	val addCue:Events[Unit]				= addCueButton.actions
	val removeCue:Events[Unit]			= removeCueButton.actions
	
	val seeking:Signal[Option[Boolean]]	=
			seekForwardButton.pressed	upDown
			seekBackwardButton.pressed
}
