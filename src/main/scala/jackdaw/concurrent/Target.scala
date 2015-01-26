package jackdaw.concurrent

trait Target[T] {
	/** does not block the caller */
	def send(message:T)
}
