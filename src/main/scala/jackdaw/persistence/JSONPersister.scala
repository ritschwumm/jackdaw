package jackdaw.persistence

import java.io.File

import scutil.implicits._
import scutil.log._

import scjson._
import scjson.codec._
import scjson.serialization._

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
		
		value					|>
		JSONIO.writeAST[T]		|>
		JSONCodec.encodePretty	|>
		{ file writeString (JSONIO.charset, _) }
	}
}
