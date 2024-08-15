package jackdaw.migration

import java.nio.file.*

import scutil.core.implicits.*
import scutil.log.*
import scutil.io.*

import scjson.ast.*
import scjson.codec.*
import scjson.converter.*
import scjson.io.*

import jackdaw.library.*

object Migration extends Logging {
	private val steps	= Vector(V4toCurrent)

	val latestVersion	= steps.last.newVersion

	def involved(tf:TrackFiles):Set[Path]	=
		for {
			step	<- steps.toSet[Migration]
			version	<- Set(step.oldVersion, step.newVersion)
		}
		yield tf.dataByVersion(version)

	def migrate(tf:TrackFiles):Unit	= {
		steps.foreach(migrateStep(tf))
	}

	//------------------------------------------------------------------------------

	private def migrateStep(tf:TrackFiles)(step:Migration):Unit	= {
		val oldData	= tf.dataByVersion(step.oldVersion)
		val newData	= tf.dataByVersion(step.newVersion)
		if (Files.exists(oldData) && !Files.exists(newData)) {
			INFO("migrating", oldData)
			migrateFile(step, oldData, newData)
		}
	}

	private def migrateFile(step:Migration, oldFile:Path, newFile:Path):Unit	= {
		val result	=
			for {
				strO	<- JsonIo.readFileString(oldFile)
				astO	<- JsonCodec.decode(strO)
				astN	<- step.convert(astO)
				strN	= JsonCodec.encodePretty(astN)
				_		= writeFile(newFile, strN)
			}
			yield ()
		result leftEffect { e =>
			// TODO e has a weird type - why?
			ERROR("cannot migrate", oldFile, e.toString)
		}
	}

	private def writeFile(file:Path, content:String):Unit	=
		MoreFiles.writeString(file, JsonIo.charset, content)
}

trait Migration {
	val oldVersion:TrackVersion
	val newVersion:TrackVersion

	def convert(old:JsonValue):Either[JsonError,JsonValue]
}
