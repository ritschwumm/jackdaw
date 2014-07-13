package djane.gui

import sc2d._

case class ButtonStyle(
	disabled:Seq[Figure],
	inactive:Seq[Figure],
	hovered:Seq[Figure],
	pressed:Seq[Figure]
)
