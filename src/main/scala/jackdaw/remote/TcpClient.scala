package jackdaw.remote

import java.net._

final class TcpClient(port:Int) {
	// TODO needs proper exception checks
	def connect():TcpConnection	=
			new TcpConnection(new Socket(InetAddress.getLoopbackAddress, port))
}
