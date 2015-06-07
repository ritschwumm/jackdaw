package jackdaw.migration

import java.io.File

import scutil.lang._

import scjson._

import jackdaw.media.Metadata
import jackdaw.data._

object V0toV1 extends Migration {
	import V0._
	import V1._
	
	val oldVersion	= V0.version
	val newVersion	= V1.version
	
	def convert(it:JSONValue):Tried[JSONInputException,JSONValue]	=
			{
				import V0.LocalProtocol._
				JSONIO.readAST[TrackDataV0](it)
			} map { it =>
				import V1.LocalProtocol._
				JSONIO.writeAST[TrackDataV1](convertValue(it))
			}
	
	private def convertValue(it:TrackDataV0):TrackDataV1	=
			TrackDataV1(
				annotation	= it.annotation,
				cuePoints	= it.cuePoints,
				rhythm		= it.raster map { jt =>
					Rhythm(
						anchor	= jt.anchor,
						measure	= jt.measure,
						schema	= Schema(
							measuresPerPhrase	= Schema.default.measuresPerPhrase,
							beatsPerMeasure		= jt.beatsPerMeasure
						)
					)
				},
				metadata	= it.metadata,
				measure		= it.measure
			)
}
