package jackdaw.gui

import java.net.URL
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.*
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.imageio.ImageIO

import scutil.core.implicits.*
import scutil.color.*
import scutil.geom.*
import scutil.gui.implicits.*
import scutil.gui.Colors

import jackdaw.BuildInfo

/** constants for gui elements */
object Style {
	def setupLnF():Unit	= {
		UIManager setLookAndFeel new MetalLookAndFeel
		/*
		// set nimbus laf
		UIManager.getInstalledLookAndFeels
		.filter		{ _.getName == "Nimbus" }
		.map		{ _.getClassName }
		.foreach	(UIManager.setLookAndFeel)
		*/
		UIManager.getDefaults.put("Panel.background",				NORMAL_BACKGROUND)
		// UIManager.getDefaults.put("Label.foreground",				STRONG_FOREGROUND)
		UIManager.getDefaults.put("TextField.background",			STRONG_BACKGROUND)
		UIManager.getDefaults.put("TextField.foreground",			STRONG_FOREGROUND)
		UIManager.getDefaults.put("TextField.selectionBackground",	STRONG_FOREGROUND)
		UIManager.getDefaults.put("TextField.selectionForeground",	STRONG_BACKGROUND)
		UIManager.getDefaults.put("TextField.caretForeground",		FOCUS_COLOR)
	}

	//------------------------------------------------------------------------------

	private val NO_COLOR			= Colors.transparentBlack

	private val DISABLED_BACKGROUND	= Color.GRAY.darker

	// private val NORMAL_FOREGROUND	= rgb"eeeeee".toColor	// light grey
	private val NORMAL_BACKGROUND	= rgb"262626".toColor	// dark grey

	private val STRONG_BACKGROUND	= Color.BLACK
	// NOTE Color.WHITE leads to a black speedDisplay on java 8
	private val STRONG_FOREGROUND	= rgb"fefefe".toColor
	private val WEAK_FOREGROUND		= rgb"d2d2d2".toColor

	// for components with STRONG_BACKGROUND
	// between STRONG_BACKGROUND and NORMAL_BACKGROUND
	private val STRONG_OUTLINE		= Color.GRAY.darker.darker
	private val STRONG_BORDER		= BorderFactory createLineBorder STRONG_OUTLINE

	// for items with a track
	// between WEAK_FOREGROUND and NORMAL_BACKGROUND
	private val WEAK_OUTLINE		= Color.GRAY
	//private val WEAK_BORDER			= BorderFactory createLineBorder STRONG_OUTLINE

	private val STRONG_FONT			= new Font("sansserif", Font.BOLD,	11)
	private val WEAK_FONT			= new Font("sansserif", Font.PLAIN,	9)

	private val STRONG_HIGHLIGHT	= rgb"ffd200".toColor	// yellow
	private val WEAK_HIGHLIGHT		= rgb"5399cf".toColor	// blue

	private val SIMPLE_STROKE		= new BasicStroke(1)
	private val FAT_STROKE			= new BasicStroke(3)

	private val FOCUS_COLOR			= Color.RED

	/** focus off, focus on */
	private def focusBorders(top:Boolean, left:Boolean, bottom:Boolean, right:Boolean, outer:Int, inner:Int):(Border,Border)	= {
		def make(size:Int, factory:(Int,Int,Int,Int)=>Border):Border	=
			factory(
				top		.cata (0, size),
				left	.cata (0, size),
				bottom	.cata (0, size),
				right	.cata (0, size)
			)
		val off	= make(inner+outer, BorderFactory.createEmptyBorder)
		val on	=
			BorderFactory.createCompoundBorder(
				make(outer, BorderFactory.createMatteBorder(_,_,_,_,FOCUS_COLOR)),
				make(inner, BorderFactory.createEmptyBorder)
			)
		(off, on)
	}

	//------------------------------------------------------------------------------

	object application {
		val icon	= bufferedImage("/logo.png")
	}

	object window {
		val title	= show"${BuildInfo.name} ${BuildInfo.version}"
		val icon	= bufferedImage("/logo.png") // imageIcon("/logo.png").getImage
		val size	= new Dimension(682, 640)
	}

	//------------------------------------------------------------------------------

	object button {
		object shape {
			object disabled {
				val color	= DISABLED_BACKGROUND
				val stroke	= FAT_STROKE
			}
			object inactive {
				val color	= WEAK_FOREGROUND
				val stroke	= FAT_STROKE
			}
			object hovered {
				val color	= inactive.color.brighter.brighter
				val stroke	= FAT_STROKE
			}
			object pressed {
				val color	= inactive.color.darker
				val stroke	= FAT_STROKE
			}

		}
		object label {
			val color	= STRONG_BACKGROUND
			val stroke	= SIMPLE_STROKE
		}
		object trial {
			val yes		= STRONG_HIGHLIGHT
			val please	= WEAK_HIGHLIGHT
			val no		= NO_COLOR
		}
		object outline {
			val color	= STRONG_OUTLINE
			val stroke	= SIMPLE_STROKE
		}
	}

	object meta {
		val border	= STRONG_BORDER

		object background {
			val color	= STRONG_BACKGROUND
		}
		object display {
			object strong {
				val font	= STRONG_FONT
				val color	= STRONG_FOREGROUND
			}
			object weak {
				val font	= WEAK_FONT
				val color	= STRONG_FOREGROUND
			}
		}
		object edit {
			object strong {
				val font	= STRONG_FONT
			}
			object weak {
				val font	= WEAK_FONT
			}
		}
	}

	object rotary {
		val	size	= new Dimension(28, 28)

		// track and knob
		object outline {
			val color	= WEAK_OUTLINE
			val stroke	= SIMPLE_STROKE
		}
		object track {
			val size	= 5
			val color	= WEAK_FOREGROUND
		}
		object active {
			val color	= WEAK_HIGHLIGHT
		}
		object knob {
			val size	= 6.0
			val color	= STRONG_HIGHLIGHT
		}

		object angle {
			val opening	= 30.0	// 45.0/2.0
			val min		= (270 - opening)
			val max		= (-90 + opening)
		}
	}

	object linear {
		val size	= new Dimension(14,14)

		// track and knob
		object outline {
			val color	= WEAK_OUTLINE
			val stroke	= SIMPLE_STROKE
		}
		object track {
			val size	= 6.0
			val color	= WEAK_FOREGROUND
		}
		object active {
			val color	= WEAK_HIGHLIGHT
		}
		object knob {
			val size	= 6.0
			val color	= STRONG_HIGHLIGHT
		}
	}

	object meter {
		val size	= new Dimension(6,6)
		val border	= STRONG_BORDER

		object track {
			val color	= STRONG_BACKGROUND
		}
		object value {
			val zero	= Color.GREEN
			val ok		= Color.GREEN
			val warn	= Color.YELLOW
			val over	= Color.RED
		}
	}

	object phase {
		val size	= new Dimension(14,14)
		val border	= STRONG_BORDER

		object background {
			val color	= STRONG_BACKGROUND
		}
		object beat {
			val color	= STRONG_FOREGROUND
			val stroke	= SIMPLE_STROKE
		}
		object tick {
			val color	= WEAK_FOREGROUND
			val stroke	= SIMPLE_STROKE
		}
		object bar {
			val color	= WEAK_HIGHLIGHT
		}
	}

	object wave {
		val border	= STRONG_BORDER

		object background {
			val color	= STRONG_BACKGROUND
		}
		object roll {
			val height	= 3
			val paint	= WEAK_HIGHLIGHT
		}
		object stacked {
			val low		= HSB(0.05f, 1f, 0.8f).toColor
			val middle	= HSB(0.15f, 1f, 0.9f).toColor
			val high	= HSB(0.25f, 1f, 1.0f).toColor
		}
		object overlap {
			val low		= HSB(0.10f, 1f, 0.50f).toColor
			val middle	= HSB(0.12f, 1f, 0.75f).toColor
			val high	= HSB(0.14f, 1f, 1.00f).toColor
		}
		object position {
			val color	= STRONG_FOREGROUND
			val stroke	= SIMPLE_STROKE
		}
		object loop {
			val color	= DISABLED_BACKGROUND withAlpha 0.4f
		}
		object marker {
			val color	= WEAK_FOREGROUND
			val stroke	= SIMPLE_STROKE
			object triangle {
				val width	= 3
				val height	= 6
			}
			object sixangle {
				val width	= 5
				val height	= 10
			}
			object rectangle {
				val width	= 4
				val height	= 7
			}
			object number {
				val stroke	= SIMPLE_STROKE
				val color	= STRONG_BACKGROUND
				val size	= IntPoint(rectangle.width-1, rectangle.height-2)
				val end		= size - IntPoint.one
			}
		}
	}

	object deck {
		object border {
			val (noFocus, inFocus)	= focusBorders(false, true, false, false, 4, 6)
		}
	}

	object channel {
		object border {
			val (noFocus, inFocus)	= focusBorders(true, false, false, false, 4, 6)
		}
	}

	object speed {
		val width	= 44

		object border {
			val (noFocus, inFocus)	= focusBorders(false, false, true, false, 4, 6)
		}
		object display {
			val font	= STRONG_FONT
			val color	= STRONG_FOREGROUND
		}
	}

	//------------------------------------------------------------------------------

	private def bufferedImage(path:String):BufferedImage	= ImageIO read resource(path)
	//private def imageIcon(path:String):ImageIcon			= new ImageIcon(resource(path))
	private def resource(path:String):URL					= getClass getResource path
}
