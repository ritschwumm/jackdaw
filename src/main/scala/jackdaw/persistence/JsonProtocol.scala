package jackdaw.persistence

import scutil.lang.*
import scutil.time.*

import scjson.converter.*
import scjson.converter.JsonFormat.{given, *}

import jackdaw.data.*
import jackdaw.media.Metadata
import jackdaw.key.*

object JsonProtocol {
	given JsonReader[MilliInstant]	= newtypeReader[MilliInstant]
	given JsonWriter[MilliInstant]	= newtypeWriter[MilliInstant]

	given JsonReader[Schema]		= cc2AutoReader[Schema]
	given JsonWriter[Schema]		= cc2AutoWriter[Schema]

	given JsonReader[Rhythm]		= cc3AutoReader[Rhythm]
	given JsonWriter[Rhythm]		= cc3AutoWriter[Rhythm]

	given JsonReader[Metadata]		= cc3AutoReader[Metadata]
	given JsonWriter[Metadata]		= cc3AutoWriter[Metadata]

	given JsonReader[MusicScale]	=
		enumReaderPf {
			case "major"	=> MusicScale.Major
			case "minor"	=> MusicScale.Minor
		}
	given JsonWriter[MusicScale]	=
		enumWriter {
			case MusicScale.Major	=> "major"
			case MusicScale.Minor	=> "minor"
		}

	given JsonReader[MusicPitch]	= newtypeReader[MusicPitch]
	given JsonWriter[MusicPitch]	= newtypeWriter[MusicPitch]

	given JsonReader[MusicChord]	= cc2AutoReader[MusicChord]
	given JsonWriter[MusicChord]	= cc2AutoWriter[MusicChord]

	given JsonReader[MusicKey]	=
		sumReaderVar(
			"silence"	-> subReader(coReader(MusicKey.Silence)),
			"chord"		-> subReader(cc1AutoReader[MusicKey.Chord]),
		)
	given JsonWriter[MusicKey]	=
		sumWriterVar(
			"silence"	-> subWriter(coWriter(MusicKey.Silence),	MusicKey.P.Silence.get),
			"chord"		-> subWriter(cc1AutoWriter[MusicKey.Chord],	MusicKey.P.Chord.get),
		)

	given[T:JsonReader]:JsonReader[Stamped[T]]	= cc2AutoReader[Stamped[T]]
	given[T:JsonWriter]:JsonWriter[Stamped[T]]	= cc2AutoWriter[Stamped[T]]

	given JsonReader[TrackData]	= cc6AutoReader[TrackData]
	given JsonWriter[TrackData]	= cc6AutoWriter[TrackData]
}
