package djane.gui

import java.awt.{ List=>AwtList, _ }
import javax.swing._

import scutil.lang._
import scutil.implicits._
import scutil.gui.CasterInstances._

import screact._

import djane.gui.action._

object TransportUI {
	private val maxCuePoints	= ButtonStyleFactory.digitCount
}

/** playback control */
final class TransportUI(playing:Signal[Boolean], afterEnd:Signal[Boolean], trackLoaded:Signal[Boolean], cuePointsCount:Signal[Int]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## input
	
	private val playingIcon	= 
			playing map { _ cata (ButtonStyleFactory.PLAY, ButtonStyleFactory.PAUSE) } 
	
	//------------------------------------------------------------------------------
	//## cue point components
	
	private val cuePointsPanel	= new JPanel
	cuePointsPanel setLayout new BoxLayout(cuePointsPanel, BoxLayout.X_AXIS)
	
	private val (cuePointComponents,cuePointActionEvents)	= 
			(cuePointsCount map { count =>
				decouple {
					val (components, actions)	= 
							(0 until count map { index =>
								val image	= ButtonStyleFactory STOP_DIGIT index
								val button	= new ButtonUI(
											ButtonStyleFactory.size,
											static(image),
											trackLoaded)
								val actions		= button.actions tag index
								val component	= button.component
								// avoid GC for the ButtonUI as long as the Component is still alive
								component putClientProperty ("SELF", button)
								(component, actions)
							})
							.unzip
					(components, actions)
				}
			})
			.unzip
	private val cuePointActions:Events[Int]	= 
			(cuePointActionEvents map Events.multiOrElse).flattenEvents
	
	cuePointComponents observeNow { components =>
		cuePointsPanel.removeAll()
		components foreach cuePointsPanel.add
		cuePointsPanel.revalidate()
		cuePointsPanel.repaint()
	}
	
	private val canPlay:Signal[Boolean]	= 
			signal {
				!afterEnd.current &&
				trackLoaded.current
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
	//## components
	
	// private val	cueStopButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.STOP),	trackLoaded)
	private val	playToggleButton	= new ButtonUI(ButtonStyleFactory.size, playingIcon,						canPlay)
	private val	seekBackwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.LEFT),	trackLoaded)
	private val	seekForwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RIGHT),	trackLoaded)
	private val	ejectButton			= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.EJECT),	trackLoaded)
	private val	addCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RECORD),	canAddCuePoint)
	private val	removeCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.CROSS),	canRemoveCuePoint)
	
	private val panel	= 
			HBoxUI(
				// cueStopButton,
				playToggleButton,
				BoxStrut(4),
				ejectButton,
				BoxStrut(4),
				seekBackwardButton,
				BoxStrut(4),
				seekForwardButton,
				BoxStrut(4),
				ejectButton,
				BoxGlue,
				cuePointsPanel:UI,
				BoxStrut(4+2),
				removeCueButton,
				BoxStrut(2),
				addCueButton
			)
	val component:JComponent	= panel.component
	
	//------------------------------------------------------------------------------
	//## output
	
	import ActionUtil._
	
	val playToggle:Events[Unit]	= playToggleButton.actions
	val eject:Events[Unit]		= ejectButton.actions
	val jumpCue:Events[Int]		= cuePointActions
	val addCue:Events[Unit]		= addCueButton.actions
	val removeCue:Events[Unit]	= removeCueButton.actions
	
	val seeking:Signal[Option[Boolean]]		=
			seekForwardButton.pressed	upDown
			seekBackwardButton.pressed
}
