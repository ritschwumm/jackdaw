package jackdaw.persistence

import java.io._

import scutil.core.implicits._
import scutil.log._

import scjson.pickle._
import scjson.io._

final class JSONPersister[T:Format] extends Persister[T] with Logging {
	def load(file:File):Option[T]	= {
		(JSONIO loadFile file)
		.failEffect { e =>
			ERROR("unmarshalling failed", file, e)
		}
		.toOption
	}
		
	def save(file:File)(value:T) {
		file.parentOption.foreach { _.mkdirs() }
		JSONIO saveFile1 (file, value, true) foreach { e =>
			ERROR("failed to write", file, e)
		}
	}
}
