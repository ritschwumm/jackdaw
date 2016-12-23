package jackdaw.migration

import java.io._

import scutil.core.implicits._
import scutil.lang._
import scutil.log._

import scjson.ast._
import scjson.codec._
import scjson.pickle._
import scjson.io._

import jackdaw.library._

object Migration extends Logging {
	private val steps	= Vector(V0toV1, V1toV2, V2toCurrent)
	
	val latestVersion	= steps.last.newVersion
	
	def involved(tf:TrackFiles):Set[File]	=
			for {
				step	<- steps.toSet[Migration]
				version	<- Set(step.oldVersion, step.newVersion)
			}
			yield tf dataByVersion version
		
	def migrate(tf:TrackFiles) {
		steps foreach migrateStep(tf)
	}
	
	//------------------------------------------------------------------------------
	
	private def migrateStep(tf:TrackFiles)(step:Migration) {
		val oldData	= tf dataByVersion step.oldVersion
		val newData	= tf dataByVersion step.newVersion
		if (oldData.exists && !newData.exists) {
			INFO("migrating", oldData)
			migrateFile(step, oldData, newData)
		}
	}
	
	private def migrateFile(step:Migration, oldFile:File, newFile:File) {
		val result	=
				for {
					strO	<- readFile(oldFile)
					astO	<- JSONCodec decode strO
					astN	<- step convert astO
					strN	= JSONCodec encodePretty astN
					_		= writeFile(newFile, strN)
				}
				yield ()
		result failEffect { e =>
			ERROR("cannot migrate", oldFile, e)
		}
	}
	
	private def readFile(file:File):Tried[JSONIOExceptionFailure,String]	=
			try {
				Win(file readString JSONIO.charset)
			}
			catch { case e:IOException =>
				Fail(JSONIOExceptionFailure(e))
			}
			
	private def writeFile(file:File, content:String):Unit	=
			file writeString (JSONIO.charset, content)
}

trait Migration {
	val oldVersion:TrackVersion
	val newVersion:TrackVersion
	
	def convert(old:JSONValue):Tried[JSONUnpickleFailure,JSONValue]
}
