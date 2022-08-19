package jackdaw.library

import java.nio.file.*

import scutil.core.implicits.*
import scutil.jdk.implicits.*
import scutil.text.Human
import scutil.log.*
import scutil.time.*
import scutil.io.*

import jackdaw.Config
import jackdaw.migration.*

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
				.sortBy	{ it => MoreFiles.lastModified(it.meta) }
				.reverse

		INFO("migrating library")
		content foreach autoMigrate

		INFO("cleaning library")
		cleanup(content)
	}

	def trackFilesFor(file:Path):TrackFiles	=
		TrackFiles(metaBase /+ localPath(file))

	def touch(tf:TrackFiles):Unit	= synchronized {
		// provide directory
		Files.createDirectories(tf.meta)
		MoreFiles.setLastModified(tf.meta, MilliInstant.now())

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
			Files.delete(it.wav)
			Files.delete(it.curve)
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
		def walk(dir:Path):Seq[TrackFiles]	=  {
			val candidate	= TrackFiles(dir)
				 if (seemsLegit(candidate))		Vector(candidate)
			else if (Files.isDirectory(dir))	MoreFiles.listFiles(dir).flattenMany flatMap walk
			else								Vector.empty
		}
		walk(metaBase)
	}

	private def seemsLegit(it:TrackFiles):Boolean	=
		Files.isDirectory(it.meta) && {
			val standardFiles	=
					Set(
						it.wav,
						it.curve,
						it.data
					)
			val migrationFiles	=
					Migration involved it
			(standardFiles ++ migrationFiles) exists { Files.isRegularFile(_) }
		}

	private def spaceNeeded(it:TrackFiles):Long	=
		sizeOrZero(it.wav)	+
		sizeOrZero(it.curve)

	private def sizeOrZero(file:Path):Long	=
		if (Files.exists(file))	Files.size(file)
		else					0

	//------------------------------------------------------------------------------

	private def localPath(file:Path):Seq[String]	=
		file
		.selfAndParentChain
		.reverse
		.zipWithIndex
		.mapFilter { case (file, index) =>
			val name	= Option(file.getFileName).map(_.toString).flatMap(_.optionNonEmpty)
			if (index == 0)	name orElse prefixPath(file.toString)
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
