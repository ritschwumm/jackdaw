package jackdaw.migration

import reflect.runtime.universe.TypeTag

import scutil.lang._
import scutil.time._

import scjson.serialization._

import jackdaw.library.TrackVersion
import jackdaw.media.Metadata
import jackdaw.data._
import jackdaw.key._

object V2 {
	val version	= TrackVersion(2)
	
	case class TrackDataV2(
		annotation:String,
		cuePoints:ISeq[Double],
		rhythm:Option[Rhythm],
		metadata:Option[Stamped[Metadata]],
		measure:Option[Stamped[Double]],
		key:Option[Stamped[MusicKeyV2]]
	)
	
	sealed trait MusicKeyV2
	case object SilenceV2									extends MusicKeyV2
	case class ChordV2(root:MusicPitch, scale:MusicScale)	extends MusicKeyV2
	
	object LocalProtocol extends FullProtocol {
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
		implicit lazy val MusicKeyV2F	=
			caseClassSumFormat[MusicKeyV2](
				"silence"	-> caseObjectFormat(SilenceV2),
				"chord"		-> caseClassFormat2(ChordV2.apply, ChordV2.unapply)
			)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat6(TrackDataV2.apply,	TrackDataV2.unapply)
	}
}
