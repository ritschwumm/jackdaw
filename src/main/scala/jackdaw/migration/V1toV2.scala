package jackdaw.migration

import scjson.ast._
import scjson.pickle._
import scjson.io.pickle._

object V1toV2 extends Migration {
	import V1._
	import V2._

	val oldVersion	= V1.version
	val newVersion	= V2.version

	def convert(it:JsonValue):Either[JsonUnpickleFailure,JsonValue]	=
		{
			import V1.LocalProtocol._
			JsonIo.readAst[TrackDataV1](it)
		} map { it =>
			import V2.LocalProtocol._
			JsonIo.writeAst[TrackDataV2](convertValue(it))
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
