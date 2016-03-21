package jackdaw.key

import java.util.Optional
import jkeyfinder._

import scaudio.sample._

object KeyDetector {
	private val scaling	= 8
	
	def findKey(sample:Sample):MusicKey	= {
		val channels		= sample.channels
		val channelCount	= sample.channels.size
		val mono	= new Channel {
			def frameCount:Int			= sample.frameCount
			def get(frame:Int):Float	= {
				var out	= 0f
				var i = 0
				while (i < channelCount) {
					out	+= channels(i) get frame
					i	+= 1
				}
				out / channelCount
			}
		}
		/*
		val audio	= new Audio {
			def frameCount	= sample.frameCount / scaling
			def frameRate	= sample.frameRate	/ scaling
			def get(frame:Int):Float	= Sinc interpolate (mono, frame, 1d / scaling)
		}
		*/
		val audio	= new Audio {
			def frameCount	= sample.frameCount
			def frameRate	= sample.frameRate
			def get(frame:Int):Float	= mono get frame
		}
		
		val result	= KeyFinder examine audio
		convertKey(result.key)
	}
	
	private def convertKey(it:Optional[Key]):MusicKey	=
			if (it.isPresent) {
				val orig	= it.get
				Chord(
					MusicChord(
						MusicPitch(orig.pitch.ordinal),
						orig.mode match {
							case Mode.MAJOR	=> Major
							case Mode.MINOR	=> Minor
						}
					)
				)
			}
			else Silence
}
