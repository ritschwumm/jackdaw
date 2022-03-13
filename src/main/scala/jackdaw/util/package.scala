package jackdaw.util

import scutil.lang.*

// TODO should we use Validated here?
type Checked[T]	= Either[Nes[String],T]
