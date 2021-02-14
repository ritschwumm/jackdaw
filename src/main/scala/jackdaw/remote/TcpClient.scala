package jackdaw.remote

import java.net._

import scutil.lang.implicits._
import scutil.lang._

object TcpClient {
	// TODO needs proper exception checks
	def open(port:Int):IoResource[TcpConnection[ToSkeleton,ToStub]]	=
		// NOTE disables nagle's algorithm:
		// batching up packets increases latency, and low latency is more important for us than throughput
		IoResource.unsafe.releasable(new Socket(InetAddress.getLoopbackAddress, port).doto(_.setTcpNoDelay(true))) map { socket =>
			new TcpConnection(
				socket,
				(input:Input)				=> input.readToSkeleton(),
				(output:Output, it:ToStub)	=> output.writeToStub(it)
			)
		}
}
