package jackdaw.player

/** detects peaks from put values, resetting after a read and turn peaks into a decaying value */
final class PeakDetector {
	private val decayMul	= 0.002f	// 0.015f	for 50ms
	private val decayAdd	= 0.004f	// 0.02f	for 50ms
	
	private var current	= 0f
	private var peak	= 0f
	
	/** lowpass-filtered peak value with different filter coefficient for raising and falling peak values */
	def decay:Float = {
		val lower	= current * (1-decayMul) - decayAdd
		current		= if (peak > lower)	peak else lower
		peak		= 0
		current
	}
	
	def put(next:Float) {
		val abs	= if (next >= 0) 	next	else -next
		peak	= if (abs > peak)	abs		else peak
	}
}
