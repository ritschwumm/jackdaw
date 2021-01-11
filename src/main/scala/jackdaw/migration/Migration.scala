package jackdaw.migration

import java.io._

import scutil.core.implicits._
import scutil.jdk.implicits._
import scutil.log._

import scjson.ast._
import scjson.codec._
import scjson.converter._
import scjson.io.converter._

import jackdaw.library._

object Migration extends Logging {
	private val steps	= Vector(V4toCurrent)

	val latestVersion	= steps.last.newVersion

	def involved(tf:TrackFiles):Set[File]	=
		for {
			step	<- steps.toSet[Migration]
			version	<- Set(step.oldVersion, step.newVersion)
		}
		yield tf dataByVersion version

	def migrate(tf:TrackFiles):Unit	= {
		steps foreach migrateStep(tf)
	}

	//------------------------------------------------------------------------------

	private def migrateStep(tf:TrackFiles)(step:Migration):Unit	= {
		val oldData	= tf dataByVersion step.oldVersion
		val newData	= tf dataByVersion step.newVersion
		if (oldData.exists && !newData.exists) {
			INFO("migrating", oldData)
			migrateFile(step, oldData, newData)
		}
	}

	private def migrateFile(step:Migration, oldFile:File, newFile:File):Unit	= {
		val result	=
			for {
				strO	<- JsonIo readFileString oldFile
				astO	<- JsonCodec decode strO
				astN	<- step convert astO
				strN	= JsonCodec encodePretty astN
				_		= writeFile(newFile, strN)
			}
			yield ()
		result leftEffect { e =>
			// TODO e has a weird type - why?
			ERROR("cannot migrate", oldFile, e.toString)
		}
	}

	private def writeFile(file:File, content:String):Unit	=
		file.writeString(JsonIo.charset, content)
}

trait Migration {
	val oldVersion:TrackVersion
	val newVersion:TrackVersion

	def convert(old:JsonValue):Either[JsonError,JsonValue]
}
