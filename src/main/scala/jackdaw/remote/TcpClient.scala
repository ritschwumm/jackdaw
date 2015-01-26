package jackdaw.remote

import java.net._

import scutil.log._

final class TcpClient(port:Int) extends Logging {
	private var connection:TcpConnection	= null
	
	// TODO needs proper exception checks
	def connect():TcpConnection	=
			new TcpConnection(new Socket(InetAddress.getLoopbackAddress, port))
}
