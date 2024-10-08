package jackdaw.gui

import java.awt.Shape

import scutil.lang.*
import scutil.gui.geom.*

import jackdaw.gui.LED.*
import jackdaw.gui.shape.*

object LEDShape {
	import LED.Number.*

	val numbers	= Vector(N0, N1, N2, N3, N4,  N5, N6, N7, N8, N9)

	def shapes(rect:SgRectangle):Seq[Shape]	= {
		import LED.Horizontal.*
		import LED.Vertical.*

		val horizontal:Map[Horizontal,Double]	=
			Map(
				L	-> rect.x.start,
				R	-> rect.x.end
			)

		val vertical:Map[Vertical,Double]	=
			Map(
				T	-> rect.y.start,
				C	-> rect.y.center,
				B	-> rect.y.end
			)

		def point(it:Point):SgPoint		= {
			val positions	= pointPositions(it)
			SgPoint(
				horizontal(positions._1),
				vertical(positions._2)
			)
		}

		def segment(it:Segment):Draft	= {
			val points	= segmentPoints(it)
			Draft(
				Nes.of(points._1, points._2).map(point)
			)
		}

		def number(it:Number):Poly	=
			Poly(
				numberSegments(it).toSeq.map(segment)
			)

		numbers.map { it => polyShape(number(it)) }
	}
}
