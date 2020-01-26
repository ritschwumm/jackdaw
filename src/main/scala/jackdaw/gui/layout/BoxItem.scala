package jackdaw.gui

object BoxItem {
	implicit def UI_is_BoxItem[T](ui:T)(implicit ev:T=>UI):BoxItem	= BoxComponent(ui)
}

sealed trait BoxItem

final case class BoxComponent(ui:UI)	extends BoxItem
final case class BoxStrut(size:Int)	extends BoxItem
case object BoxGlue				extends BoxItem
