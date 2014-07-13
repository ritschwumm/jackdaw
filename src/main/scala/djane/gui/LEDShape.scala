package djane.gui

import java.awt.Shape

import scutil.implicits._

import scgeom._

import djane.gui.LED._
import djane.gui.shape._

object LEDShape {
	val numbers	= Vector(N0, N1, N2, N3, N4,  N5, N6, N7, N8, N9) 
	
	def shapes(rect:SgRectangle):Seq[Shape]	= {
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
				
		def point(it:Point):SgPoint		=
				pointPositions(it) bimap (horizontal, vertical) into SgPoint.fromPair
		
		def segment(it:Segment):Draft	=
				Draft(false, segmentPoints(it).toSeq map point)
		
		def number(it:Number):Poly	=
				Poly(numberSegments(it).toSeq map segment)
		
		numbers map { number(_).toShape }
	}
}
