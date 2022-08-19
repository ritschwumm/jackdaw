package jackdaw.persistence

import java.nio.file.Path

trait Persister[T] {
	def load(file:Path):Option[T]
	def save(file:Path)(value:T):Unit
}
