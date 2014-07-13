package djane.model

import java.io.File

import scutil.implicits._
import scutil.io.Files._
import scutil.log._

object Storage extends Logging {
	// TODO use a better place on windows systems
	val base	= HOME / ".djane"
	val meta	= base / "meta"
	
	//------------------------------------------------------------------------------
	
	def metaDir(orig:File):File	=
			meta /+ localPath(orig)
	
	private def localPath(file:File):Seq[String]	=
			file
			.selfAndParentChain
			.reverse
			.zipWithIndex
			.map { case (file, index) =>
				val name	= file.getName.guardNonEmpty
				if (index == 0)	name orElse prefixPath(file.getPath)
				else			name
			}
			.collapse
			
	/** path element for a file system root */ 
	private def prefixPath(path:String):Option[String]	=
				 if (path == "/")					None
			else if (path == """\\""")				Some("UNC")
			else if (path matches """[A-Z]:\\""")	Some(path substring (0,1))
			else {
				ERROR(s"unexpected root path ${path}")
				None
			}
}
