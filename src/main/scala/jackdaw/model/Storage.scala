package jackdaw.model

import java.io.File

import scutil.lang.ISeq
import scutil.implicits._
import scutil.io.Files._
import scutil.log._

object Storage extends Logging {
	// TODO use a better place on windows systems
	val base	= HOME / ".jackdaw"
	val meta	= base / "meta"
	
	//------------------------------------------------------------------------------
	
	def metaDir(orig:File):File	=
			meta /+ localPath(orig)
	
	private def localPath(file:File):ISeq[String]	=
			file
			.selfAndParentChain
			.reverse
			.zipWithIndex
			.flatMap { case (file, index) =>
				val name	= file.getName.guardNonEmpty
				if (index == 0)	name orElse prefixPath(file.getPath)
				else			name
			}
			
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
