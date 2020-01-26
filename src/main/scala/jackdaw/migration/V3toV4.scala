package jackdaw.migration

import scjson.ast._
import scjson.pickle._
import scjson.io.pickle._

import jackdaw.library._
import jackdaw.data._
import jackdaw.persistence.JsonProtocol

object V3toCurrent extends Migration {
	//import V3._

	val oldVersion	= V3.version
	// TODO use V4 when available
	val newVersion	= TrackVersion(4)

	def convert(it:JsonValue):Either[JsonUnpickleFailure,JsonValue]	=
		{
			import V3.LocalProtocol._
			JsonIo.readAst[TrackData](it)
		} map { it =>
			import JsonProtocol._
			JsonIo.writeAst[TrackData](it)
		}
}
