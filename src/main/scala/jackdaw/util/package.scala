package jackdaw

import scutil.lang._

package object util {
	type Checked[T]	= Either[Nes[String],T]
}
