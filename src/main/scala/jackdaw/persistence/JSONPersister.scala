package jackdaw.persistence

import java.io.File

import scutil.base.implicits._
import scutil.core.implicits._
import scutil.lang.Charsets
import scutil.log._

import scjson.codec._
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
		
		value					|>
		JSONIO.writeAST[T]		|>
		JSONCodec.encodePretty	|>
		// TODO use JSONIO.charset
		{ file writeString (Charsets.utf_8, _) }
	}
}
