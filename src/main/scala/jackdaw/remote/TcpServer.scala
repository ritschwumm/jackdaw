package jackdaw.remote

import java.net._

final class TcpServer {
	private val serverSocket	= new ServerSocket(0, 1, InetAddress.getLoopbackAddress)
	
	val port	= serverSocket.getLocalPort
	
	// TODO needs proper exception checks
	def connect():TcpConnection[ToStub,ToSkeleton]	= {
		val connection	= new TcpConnection[ToStub,ToSkeleton](
			serverSocket.accept(),
			(input:Input)					=> input.readToStub(),
			(output:Output, it:ToSkeleton)	=> output.writeToSkeleton(it) 
		)
		serverSocket.close()
		connection
	}
}
