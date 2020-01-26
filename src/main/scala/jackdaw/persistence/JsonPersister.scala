package jackdaw.persistence

import java.io._

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.log._

import scjson.pickle._
import scjson.io.pickle._

final class JsonPersister[T:Format] extends Persister[T] with Logging {
	def load(file:File):Option[T]	= {
		(JsonIo loadFile file)
		.leftEffect { e =>
			ERROR("unmarshalling failed", file, e.toString)
		}
		.toOption
	}

	def save(file:File)(value:T):Unit	= {
		file.parentOption.foreach { _.mkdirs() }
		JsonIo saveFile (file, value, true) leftEffect {
			case JsonSaveFailure.IoException(e) => ERROR("failed to write", file, e)
		}
	}
}
