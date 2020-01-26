package jackdaw.persistence

import scutil.lang._
import scutil.time._

import scjson.converter._

import jackdaw.data._
import jackdaw.media.Metadata
import jackdaw.key._

object JsonProtocol extends JsonFormat {
	implicit lazy val MilliInstantReader:JsonReader[MilliInstant]	= bijectionReader(MilliInstant.newType)
	implicit lazy val MilliInstantWriter:JsonWriter[MilliInstant]	= bijectionWriter(MilliInstant.newType)

	implicit lazy val SchemaReader:JsonReader[Schema]				= cc2AutoReader(Schema.apply)
	implicit lazy val SchemaWriter:JsonWriter[Schema]				= cc2AutoWriter(Schema.unapply)

	implicit lazy val RhythmReader:JsonReader[Rhythm]				= cc3AutoReader(Rhythm.apply)
	implicit lazy val RhythmWriter:JsonWriter[Rhythm]				= cc3AutoWriter(Rhythm.unapply)

	implicit lazy val MetadataReader:JsonReader[Metadata]			= cc3AutoReader(Metadata.apply)
	implicit lazy val MetadataWriter:JsonWriter[Metadata]			= cc3AutoWriter(Metadata.unapply)

	implicit lazy val ScaleReader:JsonReader[MusicScale]	=
		enumReaderPf {
			case "major"	=> MusicScale.Major
			case "minor"	=> MusicScale.Minor
		}
	implicit lazy val ScaleWriter:JsonWriter[MusicScale]	=
		enumWriter {
			case MusicScale.Major	=> "major"
			case MusicScale.Minor	=> "minor"
		}

	implicit lazy val MusicPitchReader:JsonReader[MusicPitch]		= bijectionReader(Bijection.Gen[MusicPitch])
	implicit lazy val MusicPitchWriter:JsonWriter[MusicPitch]		= bijectionWriter(Bijection.Gen[MusicPitch])

	implicit lazy val MusicChordReader:JsonReader[MusicChord]		= cc2AutoReader(MusicChord.apply)
	implicit lazy val MusicChordWriter:JsonWriter[MusicChord]		= cc2AutoWriter(MusicChord.unapply)

	implicit lazy val MusicKeyReader:JsonReader[MusicKey]	=
		sumReaderVar(
			"silence"	-> subReader(coReader(MusicKey.Silence)),
			"chord"		-> subReader(cc1AutoReader(MusicKey.Chord.apply)),
		)
	implicit lazy val MusicKeyWriter:JsonWriter[MusicKey]	=
		sumWriterVar(
			"silence"	-> subWriter(coWriter(MusicKey.Silence),				MusicKey.P.Silence.get),
			"chord"		-> subWriter(cc1AutoWriter(MusicKey.Chord.unapply),	MusicKey.P.Chord.get),
		)

	implicit def StampedReader[T:JsonReader]:JsonReader[Stamped[T]]	= cc2AutoReader(Stamped.apply)
	implicit def StampedWriter[T:JsonWriter]:JsonWriter[Stamped[T]]	= cc2AutoWriter(Stamped.unapply)

	implicit lazy val TrackDataReader:JsonReader[TrackData]			= cc6AutoReader(TrackData.apply)
	implicit lazy val TrackDataWriter:JsonWriter[TrackData]			= cc6AutoWriter(TrackData.unapply)
}
