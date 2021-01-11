package jackdaw.gui

import screact._

import jackdaw.gui.util._

/** controls the playback pitch of a deck */
final class MatchUI(synced:Signal[Option[Boolean]], pitched:Signal[Boolean]) extends UI {
	//------------------------------------------------------------------------------
	//## components

	private val syncUI		= new ButtonUI(ButtonStyleFactory.size, synced map ButtonStyleFactory.TRIAL, static(true))
	private val	resetButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.CROSS),	pitched)
	private val	upButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.PLUS),	static(true))
	private val	downButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.MINUS),	static(true))
	private val	pushButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.UP),		static(true))
	private val	pullButton	= new ButtonUI(ButtonStyleFactory.size, static(ButtonStyleFactory.DOWN),	static(true))

	private val panel	=
		VBoxUI(
			syncUI,
			BoxItem.Strut(4+4),
			resetButton,
			BoxItem.Strut(4+4),
			upButton,
			BoxItem.Strut(4),
			downButton,
			BoxItem.Strut(4),
			pushButton,
			BoxItem.Strut(4+4),
			pullButton
		)
	val component	= panel.component

	//------------------------------------------------------------------------------
	//## output

	import ActionUtil._

	val syncToggle:Events[Unit]	= syncUI.actions
	val reset:Events[Unit]		= resetButton.actions

	val pitch:Signal[Option[Boolean]]	=
		upButton.pressed	upDown
		downButton.pressed

	val dragging:Signal[Option[Boolean]]	=
		pushButton.pressed	upDown
		pullButton.pressed
}
