package jackdaw.migration

import scjson.ast._
import scjson.pickle._
import scjson.io._

import jackdaw.library._
import jackdaw.data._
import jackdaw.key._
import jackdaw.persistence.JsonProtocol

object V2toCurrent extends Migration {
	import V2._
	// import V3._
	
	val oldVersion	= V2.version
	val newVersion	= TrackVersion(3)
	
	def convert(it:JsonValue):Either[JsonUnpickleFailure,JsonValue]	=
			{
				import V2.LocalProtocol._
				JsonIo.readAST[TrackDataV2](it)
			} map { it =>
				import JsonProtocol._
				JsonIo.writeAST[TrackData](convertValue(it))
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
						case SilenceV2				=> Silence
						case ChordV2(root, scale)	=> Chord(MusicChord(root, scale))
					}
				}
			)
}
