package jackdaw.remote

import java.net._

import scutil.lang.implicits._

final class TcpServer {
	private val serverSocket	=
		new ServerSocket(
			0,	// port
			1,	// backlog
			InetAddress.getLoopbackAddress
		)

	val port	= serverSocket.getLocalPort

	// TODO needs proper exception checks
	def connect():TcpConnection[ToStub,ToSkeleton]	= {
		val connection	=
			new TcpConnection[ToStub,ToSkeleton](
				// NOTE disables nagle's algorithm:
				// batching up packets increases latency, and low latency is more important for us than throughput
				serverSocket.accept().doto(_.setTcpNoDelay(true)),
				(input:Input)					=> input.readToStub(),
				(output:Output, it:ToSkeleton)	=> output.writeToSkeleton(it)
			)
		serverSocket.close()
		connection
	}
}
