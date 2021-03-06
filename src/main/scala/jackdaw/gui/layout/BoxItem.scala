package jackdaw.gui

object BoxItem {
	implicit def UI_is_BoxItem[T](ui:T)(implicit ev:T=>UI):BoxItem	= Component(ui)

	final case class Component(ui:UI)	extends BoxItem
	final case class Strut(size:Int)	extends BoxItem
	case object Glue					extends BoxItem
}

sealed trait BoxItem
