package jackdaw.gui

import javax.swing.*

import scutil.core.implicits.*
import scutil.lang.*

import screact.*

import jackdaw.data.*
import jackdaw.gui.util.*

object TransportUI {
	private val maxCuePoints	= ButtonStyleFactory.digitCount
}

/** playback control */
final class TransportUI(cueable:Signal[Boolean], playable:Signal[Boolean], playing:Signal[Boolean], afterEnd:Signal[Boolean], loopChoices:Seq[(LoopDef,Signal[Boolean])], rhythmic:Signal[Boolean], cuePointsCount:Signal[Int]) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## cue point components

	private val (cuePointUIs, cuePointActionEvents)	=
		cuePointsCount.map { count =>
			decouple {
				val (uis, actions)	=
					(0 until count).map { index =>
						val image	= ButtonStyleFactory.CUE(index)
						val button	=
							new ButtonUI(
								ButtonStyleFactory.size,
								static(image),
								cueable
							)
						val actions	= button.actions.tag(index)
						(button, actions)
					}
					.unzip
				(uis, actions)
			}
		}
		.untuple
	typed[Signal[Seq[UI]]](cuePointUIs)
	typed[Signal[Seq[Events[Int]]]](cuePointActionEvents)

	private val cuePointActions:Events[Int]	=
		cuePointActionEvents.map(Events.multiOrElse).flattenEvents

	private val cuePointsPanel:UI	=
		new SwitchUI(
			cuePointUIs.map { items =>
				new HBoxUI(items.map(BoxItem.Component.apply))
			}
		)

	private val canPlay:Signal[Boolean]	=
		signal {
			!afterEnd.current &&
			playable.current
		}

	private val canLoop:Signal[Boolean]	=
		signal {
			canPlay.current &&
			rhythmic.current
		}

	private val canAddCuePoint:Signal[Boolean]	=
		signal {
			cuePointsCount.current < TransportUI.maxCuePoints &&
			cueable.current
		}

	private val canRemoveCuePoint:Signal[Boolean]	=
		signal {
			cuePointsCount.current != 0 &&
			cueable.current
		}

	//------------------------------------------------------------------------------
	//## loop components

	val (loopSetUIs, loopSetActionEvents)	=
		(
			loopChoices.map { (choice, active) =>
				val style	=
					active.map(
						_.cata(ButtonStyleFactory.LOOP_ON(_), ButtonStyleFactory.LOOP_OFF(_)).apply(choice.measures)
					)
				val button	=
					new ButtonUI(
						size	= ButtonStyleFactory.size,
						style	= style,
						enabled	= canLoop
					)
				val action	=
					button.actions.tag(Some(choice))
				(button, action)
			}
		)
		.unzip
	val loopResetUI	=
		new ButtonUI(
			size	= ButtonStyleFactory.size,
			style	= static(ButtonStyleFactory.LOOP_RESET),
			enabled	= canLoop
		)
	private val loopUIs:Seq[UI]	=
		loopSetUIs :+ loopResetUI
	private val loopActions:Events[Option[LoopDef]]	=
		Events.multiOrElse(loopSetActionEvents)	`orElse`
		loopResetUI.actions.tag(None)

	private val loopPanel:UI	=
		new HBoxUI(
			loopUIs
			.map(BoxItem.Component.apply)
			.intersperse(BoxItem.Strut(2))
		)

	//------------------------------------------------------------------------------
	//## playing components

	private val playingIcon:Signal[ButtonStyle]	=
		playing.map(_.cata(ButtonStyleFactory.PLAY, ButtonStyleFactory.PAUSE))

	private val	playToggleButton	= new ButtonUI(ButtonStyleFactory.size, playingIcon,						canPlay)

	//------------------------------------------------------------------------------
	//## other components

	private val	seekBackwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.LEFT),	playable)
	private val	seekForwardButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RIGHT),	playable)
	private val	ejectButton			= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.EJECT),	playable)
	private val	addCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.RECORD),	canAddCuePoint)
	private val	removeCueButton		= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.CROSS),	canRemoveCuePoint)

	private val panel	=
		HBoxUI(
			// cueStopButton,
			BoxItem.Component(playToggleButton),
			BoxItem.Strut(4-2),
			BoxItem.Component(ejectButton),
			BoxItem.Strut(4),
			BoxItem.Component(seekBackwardButton),
			BoxItem.Strut(4),
			BoxItem.Component(seekForwardButton),
			BoxItem.Strut(4),
			BoxItem.Component(ejectButton),
			BoxItem.Strut(12),
			BoxItem.Component(loopPanel),
			BoxItem.Strut(12),
			BoxItem.Glue,
			BoxItem.Component(cuePointsPanel),
			BoxItem.Strut(4+2),
			BoxItem.Component(removeCueButton),
			BoxItem.Strut(2),
			BoxItem.Component(addCueButton),
		)

	val component:JComponent	= panel.component
	component.putClientProperty("STRONG_REF", this)

	//------------------------------------------------------------------------------
	//## output

	import ActionUtil.*

	val playToggle:Events[Unit]			= playToggleButton.actions
	val setLoop:Events[Option[LoopDef]]	= loopActions
	val eject:Events[Unit]				= ejectButton.actions
	val jumpCue:Events[Int]				= cuePointActions
	val addCue:Events[Unit]				= addCueButton.actions
	val removeCue:Events[Unit]			= removeCueButton.actions

	val seeking:Signal[Option[Boolean]]	=
		seekForwardButton.pressed	`upDown`
		seekBackwardButton.pressed
}
