package jackdaw.gui.shape

import scutil.lang.Nes
import scutil.gui.geom.*

object Draft {
	def of(point:SgPoint, points:SgPoint*):Draft	= Draft(Nes(point, points.toVector))
}

final case class Draft(points:Nes[SgPoint])
