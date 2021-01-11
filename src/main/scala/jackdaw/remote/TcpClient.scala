package jackdaw.remote

import java.net._

import scutil.lang.implicits._

final class TcpClient(port:Int) {
	// TODO needs proper exception checks
	def connect():TcpConnection[ToSkeleton,ToStub]	=
		new TcpConnection[ToSkeleton,ToStub](
			// NOTE disables nagle's algorithm:
			// batching up packets increases latency, and low latency is more important for us than throughput
			new Socket(InetAddress.getLoopbackAddress, port).doto(_.setTcpNoDelay(true)),
			(input:Input)				=> input.readToSkeleton(),
			(output:Output, it:ToStub)	=> output.writeToStub(it)
		)
}
