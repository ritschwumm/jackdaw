package jackdaw.remote

import java.net._

import com.twitter.chill._

final class TcpConnection(socket:Socket) {
	private val instantiator = new ScalaKryoInstantiator
	instantiator setRegistrationRequired	false
	instantiator setReferences				false

	private val inputKryo	= instantiator.newKryo() 
	private val outputKryo	= instantiator.newKryo() 

	private val input	= new Input(socket.getInputStream)
	private val output	= new Output(socket.getOutputStream)

	def dispose() {
		socket.close()
	}
	
	def receive():AnyRef	= {
		inputKryo readClassAndObject input
	}
	
	def send(msg:AnyRef) {
		outputKryo writeClassAndObject (output, msg)
		output.flush()
	}
}
