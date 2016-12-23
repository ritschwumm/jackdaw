package jackdaw.util

import java.io.File
import scutil.core.implicits._
import scutil.platform._

object AppDirs {
	def forApp(name:String):File	=
			OperatingSystem.current  match {
				case Some(Linux)	=>
					Platform.homeDir / s".$name"
				case Some(OSX)		=>
					// Platform.homeDir / "Library" / "Application Support" / name
					// Platform.homeDir / "Library" / "Cache" / name,
					Platform.homeDir / s".$name"
				case Some(Windows)	=>
					val appData	= env("LOCALAPPDATA") orElse env("APPDATA") map (new File(_))
					(appData getOrElse Platform.homeDir) / name
				case None	=>
					Platform.homeDir / name
			}
	
	// TODO scutil 0.93.0
	def env(key:String):Option[String]	= Option(System getenv key)
}
