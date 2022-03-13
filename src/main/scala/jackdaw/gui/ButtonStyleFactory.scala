package jackdaw.gui

import java.awt.{ List as _, Canvas as _, * }

import scutil.lang.implicits.*
import scutil.gui.geom.*
import scutil.gui.geom.extensions.*

import sc2d.*

import jackdaw.gui.shape.*

object ButtonStyleFactory {
	private val maximumI	= 18
	val size	= new Dimension(maximumI, maximumI)

	private val maximum	= maximumI.toDouble
	private val inset		= 3d
	private val insetter	= 2d

	private val small		= 0+inset
	private val smallish	= small+insetter
	private val smallisher	= smallish+insetter
	private val big			= maximum-inset
	private val biggish		= big-insetter
	private val biggisher	= biggish-insetter
	private val medium		= (small+big)/2
	private val seri		= Vector(small, smallish, smallisher, medium, biggisher, biggish, big)

	// protected to avoid unused warnings
	protected val Vector(a1,b1,c1,d1,e1,f1,g1)	= seri.map { SgPoint(_, small)		}: @unchecked
	protected val Vector(a2,b2,c2,d2,e2,f2,g2)	= seri.map { SgPoint(_, smallish)	}: @unchecked
	protected val Vector(a3,b3,c3,d3,e3,f3,g3)	= seri.map { SgPoint(_, smallisher)	}: @unchecked
	protected val Vector(a4,b4,c4,d4,e4,f4,g4)	= seri.map { SgPoint(_, medium)		}: @unchecked
	protected val Vector(a5,b5,c5,d5,e5,f5,g5)	= seri.map { SgPoint(_, biggisher)	}: @unchecked
	protected val Vector(a6,b6,c6,d6,e6,f6,g6)	= seri.map { SgPoint(_, biggish)	}: @unchecked
	protected val Vector(a7,b7,c7,d7,e7,f7,g7)	= seri.map { SgPoint(_, big)		}: @unchecked

	val PLAY	= outlineButtonStyle(poly(
		draft(	a1,	g4,	a7, a1	)
	))
	val PAUSE	= outlineButtonStyle(poly(
		draft(	b1,	b7	),
		draft(	f1,	f7	)
	))
	// val STOP	= outlineButtonStyle(poly(
	// 	draft(	a1,	g1,	g7,	a7, a1	)
	// ))

	val EJECT	= outlineButtonStyle(poly(
		draft(	a7,	g7	),
		draft(	b4,	d1,	f4, b4	)
	))

	val LEFT	= outlineButtonStyle(poly(
		draft(	g1,	d4,	g7	),
		draft(	d1,	a4,	d7	)
	))
	val RIGHT	= outlineButtonStyle(poly(
		draft(	a1,	d4,	a7	),
		draft(	d1,	g4,	d7	)
	))
	val UP		= outlineButtonStyle(poly(
		draft(	a7,	d4,	g7	),
		draft(	a4,	d1,	g4	)
	))
	val DOWN	= outlineButtonStyle(poly(
		draft(	a1,	d4,	g1	),
		draft(	a4,	d7,	g4	)
	))

	val PLUS	= outlineButtonStyle(poly(
		draft(	d1,	d7	),
		draft(	a4,	g4	)
	))
	val MINUS	= outlineButtonStyle(poly(
		draft(	a4,	g4	)
	))

	def LOOP_ON(digit:Int):ButtonStyle	=
		loopButtonStyle(
			poly(
				draft(	/*f1,g2*/g4, g6, f7, b7, a6, a4/*a2,b1*/	)
			),
			DIGITS lift digit
		)
	def LOOP_OFF(digit:Int):ButtonStyle	=
		loopButtonStyle(
			poly(
				draft(	e1, f1, g2, g6, f7, b7, a6, a2, b1, c1	)
			),
			DIGITS lift digit
		)
	val LOOP_RESET:ButtonStyle	= outlineButtonStyle(poly(
		draft(	a7, f7, d5	)
	))

	val RECORD	= outlineButtonStyle(poly(
		draft(
			c1,	a3,	a5,
			c7,	e7,	g5,
			g3,	e1,	c1
		)
	))
	val CROSS	= outlineButtonStyle(poly(
		draft(	a1,	g7	),
		draft(	a7,	g1	)
	))

	def CUE(digit:Int):ButtonStyle	=
		cueButtonStyle(
			poly(
				draft(	a1,	g1,	g7,	a7,	a1	)
			),
			DIGITS lift digit
		)

	def TRIAL(state:Option[Boolean]):ButtonStyle	=
		trialButtonStyle(
			// identical to RECORD
			poly(
				draft(
					c1,	a3,	a5,
					c7, e7,	g5,
					g3,	e1,	c1
				)
			),
			state
		)

	//------------------------------------------------------------------------------

	private def outlineButtonStyle(poly:Poly):ButtonStyle	=
		buttonStyle { (shapePaint, shapeStroke) =>
			val shape	= polyShape(poly)
			Vector(
				StrokeShape(shape, shapePaint, shapeStroke)
			)
		}

	// BETTER make nice and use
	/*
	private def filledButtonStyle(poly:Poly):ButtonStyle	=
		buttonStyle { (shapePaint, shapeStroke) =>
			val shape	= poly.toShape
			Vector(
				FillShape(shape, shapePaint),
				StrokeShape(shape, shapePaint, shapeStroke)
			)
		}
	*/

	// just puts a digit on top
	private def loopButtonStyle(base:Poly, digit:Option[Shape]):ButtonStyle	=
		buttonStyle { (shapePaint, shapeStroke) =>
			val shape	= polyShape(base)
			Vector(
				StrokeShape(shape, shapePaint, shapeStroke)
			) ++
			(digit map { it =>
				StrokeShape(it, shapePaint, Style.button.label.stroke)
			}).toVector
		}

	// fills the inner part
	private def cueButtonStyle(base:Poly, digit:Option[Shape]):ButtonStyle	=
		buttonStyle { (shapePaint, shapeStroke) =>
			val shape	= polyShape(base)
			Vector(
				FillShape(shape, shapePaint),
				StrokeShape(shape, shapePaint, shapeStroke)
			) ++
			(digit map { it =>
				StrokeShape(it, Style.button.label.color, Style.button.label.stroke)
			}).toVector
		}

	private def trialButtonStyle(base:Poly, state:Option[Boolean]):ButtonStyle	=
		buttonStyle { (shapePaint, shapeStroke) =>
			val shape	= polyShape(base)
			Vector(
				FillShape(shape, trialPaint(state)),
				StrokeShape(shape, shapePaint, shapeStroke)
			)
		}

	private def buttonStyle(factory:(Paint,Stroke)=>Seq[Figure]):ButtonStyle	=
		ButtonStyle(
			disabled	= factory(Style.button.shape.disabled.color,	Style.button.shape.disabled.stroke),
			inactive	= factory(Style.button.shape.inactive.color,	Style.button.shape.inactive.stroke),
			hovered		= factory(Style.button.shape.hovered.color,		Style.button.shape.hovered.stroke),
			pressed		= factory(Style.button.shape.pressed.color,		Style.button.shape.pressed.stroke)
		)

	private lazy val trialPaint:Option[Boolean]=>Paint	=
		Map(
			Some(true)	-> Style.button.trial.yes,
			Some(false)	-> Style.button.trial.please,
			None		-> Style.button.trial.no
		)

	//------------------------------------------------------------------------------

	def digitCount	= DIGITS.size

	private lazy val DIGITS:Seq[Shape]	=
		LEDShape shapes (
			(smallisher + 1	spanTo	biggisher - 1)	rectangleWith
			(smallish   + 1	spanTo	biggish   - 1)
		)
}
