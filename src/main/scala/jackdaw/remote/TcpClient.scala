package jackdaw.remote

import java.net._

final class TcpClient(port:Int) {
	// TODO needs proper exception checks
	def connect():TcpConnection[ToSkeleton,ToStub]	=
			new TcpConnection[ToSkeleton,ToStub](
				new Socket(InetAddress.getLoopbackAddress, port),
				(input:Input)				=> input.readToSkeleton(),
				(output:Output, it:ToStub)	=> output.writeToStub(it) 
			)
}
