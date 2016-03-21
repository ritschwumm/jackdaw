package jackdaw.model

import screact.{ Engine => _, _ }
import screact.swing._

import jackdaw.Config
import jackdaw.player._
import jackdaw.remote.EngineStub

/** application model */
final class Model extends Observing {
	private val engine	= new EngineStub
	
	val phoneEnabled	= engine.phoneEnabled
	
	//------------------------------------------------------------------------------
	//## receive feedback from engine
	
	// TODO lock ugly
	private val clock	= SwingClock(Config.guiUpdateInterval)
	
	private val nanoChange:Events[Long]	=
			((clock tag System.nanoTime) stateful System.nanoTime) { (old,cur) =>
				(cur, cur - old)
			}
	
	private val timedFeedback	= nanoChange map engine.feedbackTimed
	
	private val engineFeedback	= timedFeedback.filterOption hold EngineFeedback.empty
	
	// private val engineFeedback	= (clock tag engine.feedbackAll).filterOption hold EngineFeedback.empty
	private val playerFeedback1	= engineFeedback map { _.player1 }
	private val playerFeedback2	= engineFeedback map { _.player2 }
	private val playerFeedback3	= engineFeedback map { _.player3 }
	
	//------------------------------------------------------------------------------
	//## forward outgoing events from engine
	
	val masterPeak	= engineFeedback	map { _.masterPeak }
	val masterPeak1	= playerFeedback1	map { _.masterPeak }
	val masterPeak2	= playerFeedback2	map { _.masterPeak }
	val masterPeak3	= playerFeedback3	map { _.masterPeak }
	
	//------------------------------------------------------------------------------
	//## child models
	
	val mix		= new Mix
	val deck1	= new Deck(mix.strip1, mix.tone1, enqueuePlayerAction(1), playerFeedback1)
	val deck2	= new Deck(mix.strip2, mix.tone2, enqueuePlayerAction(2), playerFeedback2)
	val deck3	= new Deck(mix.strip3, mix.tone3, enqueuePlayerAction(3), playerFeedback3)
	val speed	= new Speed
	
	//------------------------------------------------------------------------------
	//## send incoming events to engine
	
	private def enqueuePlayerAction(playerId:Int)(action:PlayerAction) {
		engine enqueueAction EngineControlPlayer(playerId, action)
	}
	
	speed.beatRate	map EngineSetBeatRate.apply	observeNow engine.enqueueAction
	
	private val changeControl:Signal[EngineChangeControl]	=
			signal {
				EngineChangeControl(
					speaker	= mix.master.speakerGain.current,
					phone	= mix.master.phoneGain.current
				)
			}
	changeControl	observeNow engine.enqueueAction
	
	//------------------------------------------------------------------------------
	//## life cycle
	
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
