package jackdaw.gui.util

import java.awt.{ List=>AwtList, _ }
import java.awt.image._

import scutil.lang._
import scutil.implicits._
import scutil.geom._
import scutil.gui.implicits._

object ImageUtil {
	// TODO graco ugly
	def forComponent(component:Component):ImageUtil	=
			new ImageUtil(
				component.getGraphicsConfiguration.guardNotNull getOrElse
				defaultGraphicsConfiguration
			)
			
	private def defaultGraphicsConfiguration	=
			GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.getDefaultConfiguration
}

final class ImageUtil(graphicsConfiguration:GraphicsConfiguration) {
	def renderImage(size:IntPoint, transparent:Boolean, paint:Effect[Graphics2D]):BufferedImage	=
			graphicsConfiguration
			.createCompatibleImage(
				size.x,
				size.y,
				transparent cata (
					Transparency.OPAQUE,
					Transparency.TRANSLUCENT
				)
			)
			.doto {
				_.createGraphics.asInstanceOf[Graphics2D] use paint 
			}
	
	/*
	def renderImage(size:IntPoint, paint:Effect[Graphics2D]):BufferedImage	=
			new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_ARGB) doto {
				_.createGraphics.asInstanceOf[Graphics2D] use paint
			}
	*/
}
