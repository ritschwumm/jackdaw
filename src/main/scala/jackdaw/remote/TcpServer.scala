package jackdaw.remote

import java.net.*

import scutil.lang.implicits.*
import scutil.lang.*

object TcpServer {
	val create:IoResource[TcpServer]	=
		IoResource.unsafe.releasable(
			new ServerSocket(
				0,	// port
				1,	// backlog
				InetAddress.getLoopbackAddress
			)
		)
		.map(new TcpServer(_))
}

final class TcpServer(serverSocket:ServerSocket) {
	val port	= serverSocket.getLocalPort

	val connect:IoResource[TcpConnection[ToStub,ToSkeleton]]	=
		// NOTE disables nagle's algorithm:
		// batching up packets increases latency, and low latency is more important for us than throughput
		IoResource.unsafe.releasable(serverSocket.accept().doto(_.setTcpNoDelay(true)))
		.map { socket =>
			new TcpConnection[ToStub,ToSkeleton](
				socket,
				(input:Input)					=> input.readToStub(),
				(output:Output, it:ToSkeleton)	=> output.writeToSkeleton(it)
			)
		}
}
