package jackdaw.persistence

import reflect.runtime.universe._

import scutil.time._

import scjson.serialization._

import jackdaw.audio.Metadata
import jackdaw.data._

object JSONProtocol extends FullProtocol {
	implicit lazy val MilliInstantF			= viaFormat(MilliInstant.newType)
	implicit lazy val RhythmF				= caseClassFormat3(Rhythm.apply,		Rhythm.unapply)
	implicit lazy val MetadataF				= caseClassFormat3(Metadata.apply,		Metadata.unapply)
	implicit def StampedF[T:TypeTag:Format]	= caseClassFormat2(Stamped.apply[T],	Stamped.unapply[T])
	implicit lazy val TrackDataF			= caseClassFormat5(TrackData.apply,		TrackData.unapply)
}
