package djane.gui

import java.awt.{ List=>AwtList, Canvas=>AwtCanvas, _ }
import java.awt.geom._

import scutil.lang.ISeq
import scutil.implicits._

import scgeom._

import sc2d._

import djane.gui.shape._

object ButtonStyleFactory {
	private lazy val maximumI	= 18
	val size	= new Dimension(maximumI, maximumI)
	
	private lazy val maximum	= maximumI.toDouble
	private lazy val inset		= 3d
	private lazy val insetter	= 2d
	
	private lazy val small		= 0+inset
	private lazy val smallish	= small+insetter
	private lazy val smallisher	= smallish+insetter
	private lazy val medium		= (small+big)/2
	private lazy val biggisher	= biggish-insetter
	private lazy val biggish	= big-insetter
	private lazy val big		= maximum-inset
	
	val PLAY	= outlineButtonStyle(poly(
		closed(	(small,small),		(big,medium),		(small,big)		)
	))
	val STOP	= outlineButtonStyle(poly(
		closed(	(small,small),		(big,small),		(big,big),		(small,big)	)
	))
	val EJECT	= outlineButtonStyle(poly(
		open(	(small,big),		(big,big)),
		closed(	(smallish,medium),	(medium,small),	(biggish,medium))
	))
	val LEFT	= outlineButtonStyle(poly(
		open(	(big,small),		(medium,medium),	(big,big)		),
		open(	(medium,small),		(small,medium),		(medium,big)	)
	))
	val RIGHT	= outlineButtonStyle(poly(
		open(	(small,small),		(medium,medium),	(small,big)		),
		open(	(medium,small),		(big,medium),		(medium,big)	)
	))
	val UP		= outlineButtonStyle(poly(
		open(	(small,big),		(medium,medium),	(big,big)		),
		open(	(small,medium),		(medium,small),		(big,medium)	)
	))
	val DOWN	= outlineButtonStyle(poly(
		open(	(small,small),		(medium,medium),	(big,small)		),
		open(	(small,medium),		(medium,big),		(big,medium)	)
	))
	val PLUS	= outlineButtonStyle(poly(
		open(	(medium,small),		(medium,big)	),
		open(	(small,medium),		(big,medium)	)
	))
	val MINUS	= outlineButtonStyle(poly(
		open(	(small,medium),		(big,medium)	)
	))
	val CROSS	= outlineButtonStyle(poly(
		open(	(small,small),		(big,big)		),
		open(	(small,big),		(big,small)		)
	))
	val PAUSE	= outlineButtonStyle(poly(
		open(	(smallish,small),	(smallish,big)	),
		open(	(biggish,small),	(biggish,big)	)
	))
	val RECORD	= outlineButtonStyle(poly(
		closed(	
			(smallisher,small),	(small,smallisher),	(small,biggisher),	
			(smallisher,big),	(biggisher,big),	(big,biggisher),	
			(big,smallisher),	(biggisher,small)	
		)
	))
	
	def STOP_DIGIT(digit:Int):ButtonStyle	= {
		val shape	= poly(
			closed(	(small,small),	(big,small),		(big,big),		(small,big)	)
		)
		if (digit < DIGITS.size)	digitButtonStyle(shape, DIGITS(digit))
		else						outlineButtonStyle(shape)
	}
	
	def TRIAL(state:Option[Boolean]):ButtonStyle	=
			trialButtonStyle(
				// identical to RECORD
				poly(
					closed(
						(smallisher,small),	(small,smallisher),	(small,biggisher),	
						(smallisher,big), 	(biggisher,big),	(big,biggisher),	
						(big,smallisher),	(biggisher,small)	
					)
				),
				state
			)
	
	//------------------------------------------------------------------------------
	
	private def outlineButtonStyle(poly:Poly):ButtonStyle	= {
		val shape	= poly.toShape
		def factory(shapePaint:Paint, shapeStroke:Stroke):ISeq[Figure]	=
				Vector(
					StrokeShape(shape, shapePaint, shapeStroke)
				)
		buttonStyle(factory)
	}
	
	/*
	// BETTER make nice and use
	private def filledButtonStyle(poly:Poly):ButtonStyle	= {
		val shape	= poly.toShape
		def factory(shapePaint:Paint, shapeStroke:Stroke):ISeq[Figure]	= 
				Vector(
					 FillShape(shape)					withPaint shapePaint,
					StrokeShape(SshapeStroke, shape)	withPaint shapePaint
				)
		buttonStyle(factory)
	}
	*/
	
	private def digitButtonStyle(base:Poly, digit:Shape):ButtonStyle	= {
		val shape	= base.toShape
		def factory(shapePaint:Paint, shapeStroke:Stroke):ISeq[Figure]	= 
				Vector(
					FillShape(shape, shapePaint),
					StrokeShape(shape, shapePaint, shapeStroke),
					StrokeShape(digit, Style.button.label.color, Style.button.label.stroke)	
				)
		buttonStyle(factory)
	}
	
	private def trialButtonStyle(base:Poly, state:Option[Boolean]):ButtonStyle	= {
		val shape	= base.toShape
		def factory(shapePaint:Paint, shapeStroke:Stroke):ISeq[Figure]	= 
				Vector(
					FillShape(shape, trialPaint(state)),
					StrokeShape(shape, shapePaint, shapeStroke)
				)
		buttonStyle(factory)
	}
	
	private def buttonStyle(factory:(Paint,Stroke)=>ISeq[Figure]):ButtonStyle	=
			ButtonStyle(
				disabled	= factory(Style.button.shape.disabled.color,	Style.button.shape.disabled.stroke),
				inactive	= factory(Style.button.shape.inactive.color,	Style.button.shape.inactive.stroke),
				hovered		= factory(Style.button.shape.hovered.color,		Style.button.shape.hovered.stroke),
				pressed		= factory(Style.button.shape.pressed.color,		Style.button.shape.pressed.stroke)
			)
	
	private lazy val trialPaint:Map[Option[Boolean],Paint]	= 
			Map(
				Some(true)	-> Style.button.trial.yes,
				Some(false)	-> Style.button.trial.please,
				None		-> Style.button.trial.no
			)
	
	//------------------------------------------------------------------------------
	
	def digitCount	= DIGITS.size
	
	private lazy val DIGITS:ISeq[Shape]	=
			LEDShape shapes (
				(smallisher + 1	spanTo	biggisher - 1)	rectangleWith
				(smallish   + 1	spanTo	biggish   - 1)
			)
}
