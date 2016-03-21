package jackdaw.gui.util

import java.awt.{ List=>_, _ }
import java.awt.image._

import scutil.lang._
import scutil.implicits._
import scutil.geom._
import scutil.gui.implicits._

object ImageUtil {
	def forComponent(component:Component):ImageUtil	=
			new ImageUtil(
				component.getGraphicsConfiguration.guardNotNull getOrElse
				defaultGraphicsConfiguration
			)
			
	private def defaultGraphicsConfiguration:GraphicsConfiguration	=
			GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.getDefaultConfiguration
}

final class ImageUtil(graphicsConfiguration:GraphicsConfiguration) {
	def renderImage(size:IntPoint, transparent:Boolean, paint:Effect[Graphics2D]):BufferedImage	=
			createImage(size, transparent) doto {
				_.createGraphics.asInstanceOf[Graphics2D] use paint
			}
			
	def createImage(size:IntPoint, transparent:Boolean):BufferedImage	=
			graphicsConfiguration
			.createCompatibleImage(
				size.x,
				size.y,
				transparent cata (
					Transparency.OPAQUE,
					Transparency.TRANSLUCENT
				)
			)
	
	/*
	def renderImage(size:IntPoint, paint:Effect[Graphics2D]):BufferedImage	=
			new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_ARGB) doto {
				_.createGraphics.asInstanceOf[Graphics2D] use paint
			}
	*/
}
