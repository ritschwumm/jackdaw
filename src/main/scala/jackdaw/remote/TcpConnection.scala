package jackdaw.remote

import java.net._

final class TcpConnection[I,O](
	socket:Socket,
	inputFunc:Input=>I,
	outputFunc:(Output,O)=>Unit
) {
	private val output	= new Output(socket.getOutputStream)
	private val input	= new Input(socket.getInputStream)

	def receive():I	= {
		inputFunc(input)
	}

	def send(msg:O):Unit	= {
		outputFunc(output, msg)
		output.st.flush()
	}
}
