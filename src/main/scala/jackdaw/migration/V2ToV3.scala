package jackdaw.migration

import scjson.ast._
import scjson.pickle._
import scjson.io.pickle._

import jackdaw.data._
import jackdaw.key._
import jackdaw.persistence.JsonProtocol

object V2toV3 extends Migration {
	import V2._
	// import V3._

	val oldVersion	= V2.version
	val newVersion	= V3.version

	def convert(it:JsonValue):Either[JsonUnpickleFailure,JsonValue]	=
		{
			import V2.LocalProtocol._
			JsonIo.readAst[TrackDataV2](it)
		} map { it =>
			import JsonProtocol._
			JsonIo.writeAst[TrackData](convertValue(it))
		}

		private def convertValue(it:TrackDataV2):TrackData	=
		TrackData(
			annotation	= it.annotation,
			cuePoints	= it.cuePoints,
			rhythm		= it.rhythm,
			metadata	= it.metadata,
			measure		= it.measure,
			key			= it.key map {
				_ map {
					case SilenceV2				=> MusicKey.Silence
					case ChordV2(root, scale)	=> MusicKey.Chord(MusicChord(root, scale))
				}
			}
		)
}
