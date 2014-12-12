package jackdaw.model

import screact.{ Engine => REngine, _ }
import screact.swing._

import jackdaw.Config
import jackdaw.player._

/** application model */
final class Model extends Observing {
	// TODO lock create this in the audio thread?
	private val engine	= new Engine
	
	val phoneEnabled	= engine.phoneEnabled
	
	//------------------------------------------------------------------------------
	
	// TODO lock ugly
	private val clock	= SwingClock(Config.updateTick)
	
	private val nanoChange	= {
		var last	= System.nanoTime
		events { 
			clock.message map { _ =>
				val cur	= System.nanoTime
				val	out	= cur - last
				last	= cur
				out
			}
		}
	}
	
	private val timedFeedback	= nanoChange map engine.feedbackTimed 
	
	private val engineFeedback	= timedFeedback.filterOption hold EngineFeedback.empty
	
	// private val engineFeedback	= (clock tag engine.feedbackAll).filterOption hold EngineFeedback.empty
	private val playerFeedback1	= engineFeedback map { _.player1 }
	private val playerFeedback2	= engineFeedback map { _.player2 }
	private val playerFeedback3	= engineFeedback map { _.player3 }
	
	val mix		= new Mix
	val deck1	= new Deck(mix.strip1, mix.tone1, engine playerHandle 1, playerFeedback1)
	val deck2	= new Deck(mix.strip2, mix.tone2, engine playerHandle 2, playerFeedback2)
	val deck3	= new Deck(mix.strip3, mix.tone3, engine playerHandle 3, playerFeedback3)
	val speed	= new Speed
	
	//------------------------------------------------------------------------------
	//## outgoing
	
	val masterPeak	= engineFeedback	map { _.masterPeak }
	val masterPeak1	= playerFeedback1	map { _.masterPeak }
	val masterPeak2	= playerFeedback2	map { _.masterPeak }
	val masterPeak3	= playerFeedback3	map { _.masterPeak }
	
	//------------------------------------------------------------------------------
	//## incoming
	
	private val changeSpeed	= signal {
		EngineAction.SetBeatRate(speed.beatRate.current)
	}
	changeSpeed observeNow engine.handle
	
	private val changeControl	= signal {
		EngineAction.ChangeControl(
			speaker	= mix.master.speakerGain.current,
			phone	= mix.master.phoneGain.current
		)
	}
	changeControl observeNow engine.handle
	
	//------------------------------------------------------------------------------
	
	def start() {
		engine.start()
	}
	
	def dispose() {
		engine.dispose()
		clock.dispose()
		// NOTE Model$ keeps being referenced by Thread#contextClassLoader 
		// clock	= null
	}
}
