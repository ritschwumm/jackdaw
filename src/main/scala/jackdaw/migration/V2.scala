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
import jackdaw.persistence._

object V2 extends Migration with Logging {
	import V3._
	
	case class TrackDataV2(
		annotation:String,
		cuePoints:ISeq[Double],
		rhythm:Option[Rhythm],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]]
	)
	
	object JSONProtocolV2 extends FullProtocol {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val SchemaF				= caseClassFormat2(Schema.apply,		Schema.unapply)
		implicit lazy val RhythmF				= caseClassFormat3(Rhythm.apply,		Rhythm.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat5(TrackDataV2.apply,	TrackDataV2.unapply)
	}
	
	//------------------------------------------------------------------------------

	val oldVersion	= TrackVersion(1)
	val newVersion	= TrackVersion(2)
	
	def migrate(oldFile:File, newFile:File) {
		read(oldFile) match {
			case Fail(e)	=> ERROR("cannot migrate", oldFile, e)
			case Win(x)		=> x |> convert |> write(newFile) _
		}
	}
	
	private def read(file:File):Tried[JSONInputException,TrackDataV2]	= {
		import JSONProtocolV2._
		JSONIO.loadFile[TrackDataV2](file)
	}
	
	private def write(file:File)(data:TrackDataV3):Unit	= {
		import JSONProtocolV3._
		(new JSONPersister[TrackDataV3]).save(file)(data)
	}
	
	private def convert(it:TrackDataV2):TrackDataV3	=
			TrackDataV3(
				annotation	= it.annotation,
				cuePoints	= it.cuePoints,
				rhythm		= it.rhythm,
				metadata	= it.metadata,
				measure		= it.measure,
				key			= None
			)
}
