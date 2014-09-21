package jackdaw.gui

object BoxItem {
	implicit def UI_is_BoxItem[T<%UI](ui:T):BoxItem	= BoxComponent(ui)
}

sealed trait BoxItem

case class BoxComponent(ui:UI)	extends BoxItem
case class BoxStrut(size:Int)	extends BoxItem
case object BoxGlue				extends BoxItem
