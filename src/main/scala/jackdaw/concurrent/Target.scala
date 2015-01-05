package jackdaw.concurrent

trait Target[T] {
	def send(message:T)
}
