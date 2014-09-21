package jackdaw.player

/** what a player is doing right now */
object PlayerMode {
	/** plain motor-driven operation */
	object Playing		extends PlayerMode
	/** moving the platter manually  */
	object Scratching	extends PlayerMode
	/** brake/accelerate */
	object Dragging		extends PlayerMode
}

sealed abstract class PlayerMode
