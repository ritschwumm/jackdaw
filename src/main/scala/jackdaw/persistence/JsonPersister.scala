package jackdaw.persistence

import java.io._

import scutil.core.implicits._
import scutil.jdk.implicits._
import scutil.log._

import scjson.converter._
import scjson.io.converter._

final class JsonPersister[T:JsonReader:JsonWriter] extends Persister[T] with Logging {
	def load(file:File):Option[T]	= {
		(JsonIo loadFile file)
		.leftEffect { e =>
			ERROR("unmarshalling failed", file, e.toString)
		}
		.toOption
	}

	def save(file:File)(value:T):Unit	= {
		file.parentOption.foreach { _.mkdirs() }
		JsonIo.saveFile(file, value, true) leftEffect {
			case JsonSaveFailure.IoException(e) 	=> ERROR("failed to write", file, e)
			case JsonSaveFailure.UnparseFailure(e)	=> ERROR("failed to unparse", file, e)
		}
	}
}
