package jackdaw.migration

import scjson.ast._
import scjson.converter._
import scjson.io._

import jackdaw.library._
import jackdaw.data._
import jackdaw.persistence.JsonProtocol

object V4toCurrent extends Migration {
	//import V3._

	val oldVersion	= V4.version
	// TODO use V5 when available
	val newVersion	= TrackVersion(5)

	def convert(it:JsonValue):Either[JsonError,JsonValue]	=
		{
			import V4.LocalProtocol._
			JsonIo.readAst[TrackData](it)
		} flatMap { it =>
			import JsonProtocol._
			JsonIo.writeAst[TrackData](it)
		}
}
