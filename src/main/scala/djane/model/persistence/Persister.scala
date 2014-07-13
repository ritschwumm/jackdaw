package djane.model.persistence

import java.io.File

trait Persister[T] {
	def load(file:File):Option[T]
	def save(file:File)(value:T):Unit
}
