package jackdaw.persistence

import java.nio.file.*

import scutil.core.implicits.*
import scutil.jdk.implicits.*
import scutil.log.*
import scutil.io.*

import scjson.converter.*
import scjson.io.*

final class JsonPersister[T:JsonReader:JsonWriter] extends Persister[T] with Logging {
	def load(file:Path):Option[T]	= {
		JsonIo.loadFile(file)
		.leftEffect { e =>
			ERROR("unmarshalling failed", file, e.toString)
		}
		.toOption
	}

	def save(file:Path)(value:T):Unit	= {
		MoreFiles.createParentDirectories(file)
		JsonIo.saveFile(file, value, true) leftEffect {
			case JsonSaveFailure.IoException(e) 	=> ERROR("failed to write", file, e)
			case JsonSaveFailure.UnparseFailure(e)	=> ERROR("failed to unparse", file, e)
		}
	}
}
