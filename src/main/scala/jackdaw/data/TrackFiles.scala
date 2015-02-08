package jackdaw.data

import java.io.File

import scutil.implicits._

// TODO library rename TrackFiles to LibraryItem or something like this?

object TrackFiles {
	val latest	= TrackVersion(1)
}

case class TrackFiles(meta:File) {
	val wav:File	= meta / "sample.wav"
	val curve:File	= meta / "bandCurve.bin"
	val data:File	= dataByVersion(TrackFiles.latest)
	
	def dataByVersion(version:TrackVersion):File	=
			if (version == TrackVersion(0))	meta / "data.json"
			else							meta / s"data-v${version.value}.json"
}
