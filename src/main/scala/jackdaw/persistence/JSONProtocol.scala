package jackdaw.persistence

import reflect.runtime.universe._

import scutil.lang._
import scutil.time._

import scjson.pickle._
import scjson.pickle.protocol.old._

import jackdaw.data._
import jackdaw.media.Metadata
import jackdaw.key._

object JSONProtocol extends OldFullProtocol {
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
	implicit lazy val MusicChordF			= caseClassFormat2(MusicChord.apply,	MusicChord.unapply)
	implicit lazy val MusicKeyF	=
			caseClassSumFormat[MusicKey](
				"silence"	-> caseObjectFormat(Silence),
				"chord"		-> caseClassFormat1(Chord.apply, Chord.unapply)
			)
	implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
	implicit lazy val TrackDataF			= caseClassFormat6(TrackData.apply,		TrackData.unapply)
}
