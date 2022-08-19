package jackdaw.library

import java.nio.file.Path

import scutil.core.implicits.*
import scutil.jdk.implicits.*

import jackdaw.migration.Migration

final case class TrackFiles(meta:Path) {
	val wav:Path	= meta / "sample.wav"
	val curve:Path	= meta / "bandCurve2.bin"
	val data:Path	= dataByVersion(Migration.latestVersion)

	def dataByVersion(version:TrackVersion):Path	=
		version match {
			case TrackVersion(0)	=> meta / "data.json"
			case TrackVersion(x)	=> meta / show"data-v${x}.json"
		}
}
