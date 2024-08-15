package jackdaw.util

import java.nio.file.Path

import scutil.jdk.implicits.*
import scutil.platform.*

object AppDirs {
	def forApp(name:String):Path	=
		OperatingSystem.current  match {
			case Some(OperatingSystem.Linux)	=>
				Platform.homeDir / ("." + name)
			case Some(OperatingSystem.OSX)		=>
				// Platform.homeDir / "Library" / "Application Support" / name
				// Platform.homeDir / "Library" / "Cache" / name,
				Platform.homeDir / ("." + name)
			case Some(OperatingSystem.Windows)	=>
				val appData	= Platform.env("LOCALAPPDATA") `orElse` Platform.env("APPDATA") `map` (Path.of(_))
				appData.getOrElse(Platform.homeDir) / name
			case None	=>
				Platform.homeDir / name
		}
}
