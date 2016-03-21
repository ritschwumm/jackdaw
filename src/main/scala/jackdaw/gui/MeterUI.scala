package jackdaw.gui

import java.awt.{ List=>_, Canvas=>_, _ }
import javax.swing._
import java.awt.image._

import scala.math._

import scutil.lang._
import scutil.implicits._
import scutil.geom._

import screact._
import scgeom._
import sc2d._

import scaudio.math._

import jackdaw.range.MeterRange
import jackdaw.gui.util._

/** value is linear [0..1] and displayed in fader values */
final class MeterUI(value:Signal[Float], meterRange:MeterRange, vertical:Boolean) extends UI with Observing {
	//------------------------------------------------------------------------------
	//## value
	
	private val linear2norm:Endo[Double]	=
			it =>
					(it					- meterRange.zero) /
					(meterRange.over	- meterRange.zero)
					
	private val linear2fraction:Endo[Double]	=
			linear2norm andThen gammaFade(-0.66)
	
	private val active	=
			Array(
				meterRange.zero		-> Style.meter.value.zero,
				meterRange.ok		-> Style.meter.value.ok,
				meterRange.warn		-> Style.meter.value.warn,
				meterRange.over		-> Style.meter.value.over
			)
			
	private val gradientColors	=
			active map { case (_, color)	=> color }
		
	private val gradientFractions	=
			active map { case (value, _)	=>
				linear2fraction(value).toFloat
			}
	
	//------------------------------------------------------------------------------
	//## components
	
	private val canvas	= new CanvasWrapper(None, Hints.lowQuality)
	
	val component:JComponent	= canvas.component
	component setBorder	Style.meter.border
	
	//------------------------------------------------------------------------------
	//## input
	
	private val orientation	= SgOrientation trueVertical vertical
	private val miniMax		= SgSpan.one
	
	private val fraction2gui:Signal[SgSpanTransform]	=
			canvas.bounds map { it =>
				SgSpanTransform fromSpans (
						orientation cata (miniMax, miniMax.swap),
						it get orientation)
			}
	
	private val activeImage:Signal[BufferedImage]	=
			canvas.bounds map { it =>
				val size	=
						IntPoint(
							it.x.size.toInt guardBy { _ > 0 } getOrElse 1,
							it.y.size.toInt guardBy { _ > 0 } getOrElse 1
						)
				
				// NOTE for y start and end are swapped
				val gradient	= new LinearGradientPaint(
							0,
							size.y,
							size.x,
							0,
							gradientFractions,
							gradientColors)
				
				ImageUtil forComponent component renderImage (size, false, g => {
					g setPaint gradient
					g fillRect	(0,0, size.x, size.y)
				})
			}
	
	//------------------------------------------------------------------------------
	//## figures
	
	private val figures:Signal[ISeq[Figure]]	=
			signal {
				val componentBoundsCur	= canvas.bounds.current
				val activeImageCur		= activeImage.current
				// LinearGradientPaint does not like an empty rectangle
				if (componentBoundsCur.empty || activeImageCur == null) {
					ISeq.empty
				}
				else {
					val fraction2guiCur	= fraction2gui.current
					val valueCur		= value.current
					
					// NOTE rint is a hack to make it change less often
					val fader		= rint(fraction2guiCur(linear2fraction(valueCur)))
					val mini		= fraction2guiCur(0)
					val maxi		= fraction2guiCur(1)
					
					val miniFader	= SgSpan(mini, fader)
					val faderMax	= SgSpan(fader, maxi)
					
					val inactive	= stripeShape(componentBoundsCur, faderMax)
					val active		= stripeShape(componentBoundsCur, miniFader)
					
					/*
					// NOTE for y start and end are swapped
					val gbounds		= componentBoundsCur set (orientation.opposite, SgSpan.zero)
					val gradient	= new LinearGradientPaint(
							gbounds.x.start.toFloat,
							gbounds.y.end.toFloat,
							gbounds.x.end.toFloat,
							gbounds.y.start.toFloat,
							MeterUI.gradientFractions,
							MeterUI.gradientColors)
					*/
					
					Vector(
						FillShape(inactive, Style.meter.track.color),
						/*
						FillShape(active) withPaint gradient,
						*/
						WithClip(
							active,
							DrawImage(
								activeImageCur,
								componentBoundsCur.x.start.toInt,
								componentBoundsCur.y.start.toInt
							)
						)
					)
				}
			}
	
	private def stripeShape(bounds:SgRectangle, span:SgSpan):Shape	=
			GeomUtil normalRectangle (
				bounds set (
					orientation,
					span
				)
			)
	
	//------------------------------------------------------------------------------
	//## wiring
	
	figures observe canvas.figures.set
}
