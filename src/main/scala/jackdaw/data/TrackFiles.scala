package jackdaw.data

import java.io.File

import scutil.implicits._

// TODO library rename TrackFiles to LibraryItem or something like this?

object TrackFiles {
	private val data	= "data.json"
	private val wav		= "sample.wav"
	private val curve	= "bandCurve.bin"
}

case class TrackFiles(meta:File) {
	def data:File	= meta / TrackFiles.data
	def wav:File	= meta / TrackFiles.wav
	def curve:File	= meta / TrackFiles.curve
}
