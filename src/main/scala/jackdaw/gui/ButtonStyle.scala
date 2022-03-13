package jackdaw.gui

import sc2d.*

final case class ButtonStyle(
	disabled:Seq[Figure],
	inactive:Seq[Figure],
	hovered:Seq[Figure],
	pressed:Seq[Figure]
)
