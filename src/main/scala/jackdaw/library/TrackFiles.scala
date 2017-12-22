package jackdaw.library

import java.io.File

import scutil.base.implicits._
import scutil.core.implicits._

import jackdaw.migration.Migration

final case class TrackFiles(meta:File) {
	val wav:File	= meta / "sample.wav"
	val curve:File	= meta / "bandCurve2.bin"
	val data:File	= dataByVersion(Migration.latestVersion)
	
	def dataByVersion(version:TrackVersion):File	=
			version match {
				case TrackVersion(0)	=> meta / "data.json"
				case TrackVersion(x)	=> meta / show"data-v${x}.json"
			}
}
