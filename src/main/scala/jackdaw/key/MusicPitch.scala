package jackdaw.key

import scutil.math._

/** index in 0..11, starting at A */
final case class MusicPitch(index:Int) {
	def move(offset:Int):MusicPitch	=
			MusicPitch(moduloInt(index + offset, 12))
}
