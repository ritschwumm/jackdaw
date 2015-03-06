package jackdaw.migration

import java.io.File

import reflect.runtime.universe._

import scutil.lang._
import scutil.implicits._
import scutil.time._
import scutil.log._

import scjson._
import scjson.serialization._

import jackdaw.media.Metadata
import jackdaw.library._
import jackdaw.data._
import jackdaw.key._
import jackdaw.persistence._

object V3 extends Migration with Logging {
	case class TrackDataV3(
		annotation:String,
		cuePoints:ISeq[Double],
		rhythm:Option[Rhythm],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]],
		key:Option[Stamped[MusicKeyV3]]
	)
	
	sealed trait MusicKeyV3
	case object SilenceV3									extends MusicKeyV3
	case class ChordV3(root:MusicPitch, scale:MusicScale)	extends MusicKeyV3
	
	object JSONProtocolV3 extends FullProtocol {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val SchemaF				= caseClassFormat2(Schema.apply,		Schema.unapply)
		implicit lazy val RhythmF				= caseClassFormat3(Rhythm.apply,		Rhythm.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit lazy val ScaleF	=
				enumFormat[MusicScale](ISeq(
					"major"	-> Major,
					"minor"	-> Minor
				))
		implicit lazy val MusicPitchF			= viaFormat(Bijector[MusicPitch])
		implicit lazy val MusicKeyV3F	=
			caseClassSumFormat[MusicKeyV3](
				"silence"	-> caseObjectFormat(SilenceV3),
				"chord"		-> caseClassFormat2(ChordV3.apply, ChordV3.unapply)
			)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat6(TrackDataV3.apply,	TrackDataV3.unapply)
	}
	
	//------------------------------------------------------------------------------

	val oldVersion	= TrackVersion(2)
	val newVersion	= TrackVersion(3)
	
	def migrate(oldFile:File, newFile:File) {
		read(oldFile) match {
			case Fail(e)	=> ERROR("cannot migrate", oldFile, e)
			case Win(x)		=> x |> convert |> write(newFile) _
		}
	}
	
	private def read(file:File):Tried[JSONInputException,TrackDataV3]	= {
		import JSONProtocolV3._
		JSONIO.loadFile[TrackDataV3](file)
	}
	
	private def write(file:File)(data:TrackData):Unit	= {
		import JSONProtocol._
		(new JSONPersister[TrackData]).save(file)(data)
	}
	
	private def convert(it:TrackDataV3):TrackData	=
			TrackData(
				annotation	= it.annotation,
				cuePoints	= it.cuePoints,
				rhythm		= it.rhythm,
				metadata	= it.metadata,
				measure		= it.measure,
				key			= it.key map {
					_ map {
						case SilenceV3				=> Silence
						case ChordV3(root, scale)	=> Chord(MusicChord(root, scale))
					}
				}
			)
}
