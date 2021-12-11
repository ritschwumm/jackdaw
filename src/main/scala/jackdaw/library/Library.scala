package jackdaw.library

import java.io.File

import scutil.core.implicits._
import scutil.jdk.implicits._
import scutil.text.Human
import scutil.log._
import scutil.time._

import jackdaw.Config
import jackdaw.migration._

object Library extends Logging {
	private val metaBase	= Config.dataBase / "meta"

	/** lately modified metadata first */
	private var content:Seq[TrackFiles]	= Vector.empty

	//------------------------------------------------------------------------------
	//## public api

	def init():Unit	= synchronized {
		INFO("scanning library")
		// this is done once a startup because scanning the filesystem later
		// will wreck havoc with the os scheduler
		content	= findTrackFiles()
				.sortBy	{ _.meta.lastModifiedMilliInstant() }
				.reverse

		INFO("migrating library")
		content foreach autoMigrate

		INFO("cleaning library")
		cleanup(content)
	}

	def trackFilesFor(file:File):TrackFiles	=
		TrackFiles(metaBase /+ localPath(file))

	def touch(tf:TrackFiles):Unit	= synchronized {
		// provide directory
		tf.meta.mkdirs()
		tf.meta setLastModifiedMilliInstant MilliInstant.now()

		// migrate if necessary
		autoMigrate(tf)

		// move touched TF to the front of the queue so they are kept
		content	= tf +: (content filterNot { _.meta ==== tf.meta })

		INFO("cleaning library")
		cleanup(content)
	}

	//------------------------------------------------------------------------------

	private def autoMigrate(tf:TrackFiles):Unit	=
		synchronized {
			Migration migrate tf
		}

	/** lately modified tracks come first in the list */
	private def cleanup(allTracks:Seq[TrackFiles]):Unit	= {
		// how many to keep, prefers the ones early in the list
		val (keepTracks, deleteTracks)	= {
			val needs	= allTracks map spaceNeeded
			val sums	= (needs scanLeft 0L) { _+_ }
			val keeps	= sums.tail takeWhile { _ < Config.maxCacheSize }
			val pos		= keeps.size max Config.minCacheCount
			allTracks splitAt pos
		}

		deleteTracks foreach { it =>
			it.wav.delete()
			it.curve.delete()
		}

		// inform user
		if (deleteTracks.nonEmpty) {
			INFO(
				show"cleaned library",
				show"was ${info(allTracks)}",
				show"kept ${info(keepTracks)}",
				show"deleted ${info(deleteTracks)}"
			)
		}
		else {
			INFO(
				show"library untouched",
				show"was ${info(allTracks)}"
			)
		}
	}

	private def info(tracks:Seq[TrackFiles]):String	= {
		val count	= tracks.size
		val space	= (tracks map spaceNeeded).sum
		(Human roundedBinary space) + "B for " +
		count.toString + " " + (count == 1).cata("tracks", "track")
	}

	/** first the ones where metadata was modified first */
	private def findTrackFiles():Seq[TrackFiles]	= {
		def walk(dir:File):Seq[TrackFiles]	=  {
			val candidate	= TrackFiles(dir)
				 if (seemsLegit(candidate))	Vector(candidate)
			else if (dir.isDirectory)		dir.children.flattenMany flatMap walk
			else							Vector.empty
		}
		walk(metaBase)
	}

	private def seemsLegit(it:TrackFiles):Boolean	=
		it.meta.isDirectory && {
			val standardFiles	=
					Set(
						it.wav,
						it.curve,
						it.data
					)
			val migrationFiles	=
					Migration involved it
			(standardFiles ++ migrationFiles) exists { _.isFile }
		}

	private def spaceNeeded(it:TrackFiles):Long	=
		it.wav.length	+
		it.curve.length

	//------------------------------------------------------------------------------

	private def localPath(file:File):Seq[String]	=
		file
		.selfAndParentChain
		.reverse
		.zipWithIndex
		.mapFilter { case (file, index) =>
			val name	= file.getName.optionNonEmpty
			if (index == 0)	name orElse prefixPath(file.getPath)
			else			name
		}

	/** path element for a file system root */
	private def prefixPath(path:String):Option[String]	=
		if		(path == "/")					None
		else if	(path == """\\""")				Some("UNC")
		else if	(path matches """[A-Z]:\\""")	Some(path.substring(0,1))
		else {
			ERROR(show"unexpected root path ${path}")
			None
		}
}
