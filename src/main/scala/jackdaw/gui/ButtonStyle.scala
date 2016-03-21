package jackdaw.gui

import scutil.lang.ISeq

import sc2d._

final case class ButtonStyle(
	disabled:ISeq[Figure],
	inactive:ISeq[Figure],
	hovered:ISeq[Figure],
	pressed:ISeq[Figure]
)
