package jackdaw

import scutil.lang._

package object util {
	// TODO should we use Validated here?
	type Checked[T]	= Either[Nes[String],T]
}
