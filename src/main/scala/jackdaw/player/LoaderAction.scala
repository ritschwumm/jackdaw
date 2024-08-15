package jackdaw.player

import java.nio.file.Path

import scutil.lang.*

enum LoaderAction {
	case Decode(file:Path, done:Effect[Option[CacheSample]])
	case Preload(sample:CacheSample, centerFrame:Int)
	case NotifyEngine(done:Thunk[Unit])
}
