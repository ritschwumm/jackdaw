package jackdaw.migration

import java.io.File

import jackdaw.library._

object Migration {
	val all	= Vector(V1, V2)
}

trait Migration {
	val oldVersion:TrackVersion
	val newVersion:TrackVersion
	def migrate(oldFile:File, newFile:File):Unit
}
