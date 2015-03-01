package jackdaw.library

import java.io.File

import scutil.implicits._

object TrackFiles {
	val latest	= TrackVersion(2)
}

case class TrackFiles(meta:File) {
	val wav:File	= meta / "sample.wav"
	val curve:File	= meta / "bandCurve.bin"
	val data:File	= dataByVersion(TrackFiles.latest)
	
	def dataByVersion(version:TrackVersion):File	=
			version match {
				case TrackVersion(0)	=> meta / "data.json"
				case TrackVersion(x)	=> meta / s"data-v${x}.json"
			}
}
