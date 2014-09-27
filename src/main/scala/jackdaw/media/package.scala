package jackdaw

import scutil.lang._

package object media {
	type Checked[T]	= Tried[ISeq[String],T]
}
