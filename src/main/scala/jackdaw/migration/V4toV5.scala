package jackdaw.migration

import scjson.ast.*
import scjson.converter.*
import scjson.io.*

import jackdaw.library.*
import jackdaw.data.*
import jackdaw.persistence.JsonProtocol

object V4toCurrent extends Migration {
	//import V3.*

	val oldVersion	= V4.version
	// TODO use V5 when available
	val newVersion	= TrackVersion(5)

	def convert(it:JsonValue):Either[JsonError,JsonValue]	=
		{
			import V4.LocalProtocol.given
			JsonIo.readAst[TrackData](it)
		}
		.flatMap { it =>
			import JsonProtocol.given
			JsonIo.writeAst[TrackData](it)
		}
}
