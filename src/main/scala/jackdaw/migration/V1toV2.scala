package jackdaw.migration

import scutil.lang._

import scjson.ast._
import scjson.io._
import scjson.pickle._

object V1toV2 extends Migration {
	import V1._
	import V2._
	
	val oldVersion	= V1.version
	val newVersion	= V2.version
	
	def convert(it:JSONValue):Tried[JSONUnpickleFailure,JSONValue]	=
			{
				import V1.LocalProtocol._
				JSONIO.readAST[TrackDataV1](it)
			} map { it =>
				import V2.LocalProtocol._
				JSONIO.writeAST[TrackDataV2](convertValue(it))
			}
	
	private def convertValue(it:TrackDataV1):TrackDataV2	=
			TrackDataV2(
				annotation	= it.annotation,
				cuePoints	= it.cuePoints,
				rhythm		= it.rhythm,
				metadata	= it.metadata,
				measure		= it.measure,
				key			= None
			)
}
