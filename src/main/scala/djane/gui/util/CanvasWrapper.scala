package djane.gui.util

import java.awt.{ List=>AwtList, Canvas=>AwtCanvas, _ }
import java.awt.event._

import screact._
import scgeom._
import sc2d._

final class CanvasWrapper(background:Option[Paint], hints:Hints) extends Observing {
	val component	= new Canvas(background, hints)
	val mouse		= new Mouse(component)
	val bounds		= ComponentUtil innerSgRectangleSignal component
	val figures		= cell(Seq.empty[Figure])
	
	figures observeNow component.setFigures
}
