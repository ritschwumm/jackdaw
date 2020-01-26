package jackdaw.migration

import reflect.runtime.universe.TypeTag

import scutil.lang._
import scutil.time._

import scjson.pickle._
import scjson.pickle.protocol.old._

import jackdaw.library.TrackVersion
import jackdaw.media.Metadata
import jackdaw.data._
import jackdaw.key._

object V3 {
	val version	= TrackVersion(3)

	object LocalProtocol extends OldStandardProtocol2 {
		implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
		implicit lazy val SchemaF				= caseClassFormat2(Schema.apply,		Schema.unapply)
		implicit lazy val RhythmF				= caseClassFormat3(Rhythm.apply,		Rhythm.unapply)
		implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
		implicit lazy val ScaleF	=
			enumFormat[MusicScale](Seq(
				"major"	-> MusicScale.Major,
				"minor"	-> MusicScale.Minor
			))
		implicit lazy val MusicPitchF			= viaFormat(Bijection.Gen[MusicPitch])
		implicit lazy val MusicChordF			= caseClassFormat2(MusicChord.apply,	MusicChord.unapply)
		implicit lazy val MusicKeyF	=
			caseClassSumFormat[MusicKey](
				"silence"	-> caseObjectFormat(MusicKey.Silence),
				"chord"		-> caseClassFormat1(MusicKey.Chord.apply, MusicKey.Chord.unapply)
			)
		implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
		implicit lazy val TrackDataF			= caseClassFormat6(TrackData.apply,		TrackData.unapply)
	}
}
