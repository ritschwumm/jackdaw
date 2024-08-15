package jackdaw.gui.util

import java.awt.Shape

import scutil.lang.*
import scutil.math.functions.*
import scutil.gui.geom.*

/** swing geometry utility functions */
object GeomUtil {
	val bottomRightInsets	= SgRectangleInsets.symmetric(SgSpanInsets.startEnd(0, 1))

	def normalRectangle(bounds:SgRectangle):Shape	=
		bounds.normalize.toAwtRectangle2D

	def smallerRectangle(bounds:SgRectangle):Shape	=
		bounds.normalize.inset(bottomRightInsets).toAwtRectangle2D

	def spanDeflate(span:SgSpan, size:Double):SgSpan	=
		SgSpan.startEnd(span.start + size/2, span.end - size/2)

	def clampValue(span:SgSpan, value:Double):Double	=
		if (span.normal)	clampDouble(value, span.start, span.end)
		else				clampDouble(value, span.start, span.end)

	def containsInclusive(span:SgSpan, epsilon:Double):Predicate[Double]	= {
		val	normal	= span.normalize
		val min		= normal.start	- epsilon
		val max		= normal.end	+ epsilon
		value => value >= min && value <= max
	}
}
