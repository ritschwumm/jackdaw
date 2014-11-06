package jackdaw

import scutil.lang._

package object util {
	type Checked[T]	= Tried[Nes[String],T]
}
