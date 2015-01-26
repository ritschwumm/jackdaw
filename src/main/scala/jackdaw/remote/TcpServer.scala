package jackdaw.remote

import java.net._

final class TcpServer {
	private val serverSocket	= new ServerSocket(0, 1, InetAddress.getLoopbackAddress)
	val port	= serverSocket.getLocalPort
	
	// TODO needs proper exception checks
	def connect():TcpConnection	= {
		val connection	= new TcpConnection(serverSocket.accept())
		serverSocket.close()
		connection
	}
}
