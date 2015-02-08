package jackdaw.migration

import java.io.File

import reflect.runtime.universe._

import scutil.lang._
import scutil.implicits._
import scutil.time._
import scutil.log._

import scjson._
import scjson.serialization._

import jackdaw.audio.Metadata
import jackdaw.data._
import jackdaw.persistence._

object V1 extends Logging {
	case class TrackDataV1(
		annotation:String,
		cuePoints:ISeq[Double],
		raster:Option[RhythmV1],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]]
	)
	
	case class RhythmV1(anchor:Double, measure:Double, beatsPerMeasure:Int)
	
	object JSONProtocolV1 extends FullProtocol {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val RhythmF				= caseClassFormat3(RhythmV1.apply,		RhythmV1.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat5(TrackDataV1.apply,	TrackDataV1.unapply)
	}
	
	//------------------------------------------------------------------------------

	val oldVersion	= TrackVersion(0)
	val newVersion	= TrackVersion(1)
	
	def migrate(oldFile:File, newFile:File) {
		read(oldFile) match {
			case Fail(e)	=> ERROR("cannot migrate", oldFile, e)
			case Win(x)		=> x |> convert |> write(newFile) _
		}
	}
	
	private def read(file:File):Tried[JSONInputException,TrackDataV1]	= {
		import JSONProtocolV1._
		JSONIO.loadFile[TrackDataV1](file)
	}
	
	private def write(file:File)(data:TrackData):Unit	= {
		import JSONProtocol._
		(new JSONPersister[TrackData]).save(file)(data)
	}
	
	private def convert(it:TrackDataV1):TrackData	=
			TrackData(
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
