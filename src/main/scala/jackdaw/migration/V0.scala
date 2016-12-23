package jackdaw.migration

import reflect.runtime.universe.TypeTag

import scutil.lang._
import scutil.time._

import scjson.pickle._
import scjson.pickle.protocol.old._

import jackdaw.library.TrackVersion
import jackdaw.media.Metadata
import jackdaw.data._

object V0 {
	val version	= TrackVersion(0)
	
	final case class TrackDataV0(
		annotation:String,
		cuePoints:ISeq[Double],
		raster:Option[RhythmV0],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]]
	)
	
	final case class RhythmV0(anchor:Double, measure:Double, beatsPerMeasure:Int)
	
	object LocalProtocol extends OldFullProtocol {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val RhythmF				= caseClassFormat3(RhythmV0.apply,		RhythmV0.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat5(TrackDataV0.apply,	TrackDataV0.unapply)
	}
}
