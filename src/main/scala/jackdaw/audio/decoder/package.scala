package jackdaw.audio

import scutil.implicits._
import scutil.lang._

package object decoder {
	type Checked[T]	= Tried[ISeq[String],T]
}
