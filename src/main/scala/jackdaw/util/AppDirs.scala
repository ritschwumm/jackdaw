package jackdaw.util

import java.io.File
import scutil.jdk.implicits.*
import scutil.platform.*

object AppDirs {
	def forApp(name:String):File	=
		OperatingSystem.current  match {
			case Some(Linux)	=>
				Platform.homeDir / ("." + name)
			case Some(OSX)		=>
				// Platform.homeDir / "Library" / "Application Support" / name
				// Platform.homeDir / "Library" / "Cache" / name,
				Platform.homeDir / ("." + name)
			case Some(Windows)	=>
				val appData	= (Platform env "LOCALAPPDATA") orElse (Platform env "APPDATA") map (new File(_))
				(appData getOrElse Platform.homeDir) / name
			case None	=>
				Platform.homeDir / name
		}
}
