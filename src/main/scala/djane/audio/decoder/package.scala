package djane.audio

import scutil.lang._

package object decoder {
	type Checked[T]	= Tried[Seq[String],T]
}
