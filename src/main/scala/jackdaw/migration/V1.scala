package jackdaw.migration

import reflect.runtime.universe.TypeTag

import scutil.lang._
import scutil.time._

import scjson.pickle._
import scjson.pickle.protocol.old._

import jackdaw.library.TrackVersion
import jackdaw.media.Metadata
import jackdaw.data._

object V1 {
	val version	= TrackVersion(1)
	
	final case class TrackDataV1(
		annotation:String,
		cuePoints:ISeq[Double],
		rhythm:Option[Rhythm],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]]
	)
	
	object LocalProtocol extends OldFullProtocol {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val SchemaF				= caseClassFormat2(Schema.apply,		Schema.unapply)
		implicit lazy val RhythmF				= caseClassFormat3(Rhythm.apply,		Rhythm.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat5(TrackDataV1.apply,	TrackDataV1.unapply)
	}
}
