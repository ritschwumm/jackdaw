package jackdaw.player

/** what a player is doing right now */
enum PlayerMode {
	/** plain motor-driven operation */
	case Playing
	/** moving the platter manually  */
	case Scratching
	/** brake/accelerate */
	case Dragging

}
