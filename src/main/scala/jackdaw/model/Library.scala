package jackdaw.model

import java.io.File

import scutil.lang._
import scutil.implicits._
import scutil.text.Human
import scutil.log._
import scutil.time._

import jackdaw.Config
import jackdaw.data._
import jackdaw.migration._

object Library extends Logging {
	def migrateAll() {
		INFO("migrating library")
		findTrackFiles() foreach autoMigrate
	}
	
	private def autoMigrate(tf:TrackFiles):Unit	= synchronized {
		Migration.all foreach { autoMigrateStep(tf, _) }
	}
	
	private def autoMigrateStep(tf:TrackFiles, migration:Migration):Unit	= synchronized {
		val oldData	= tf dataByVersion migration.oldVersion
		val newData	= tf dataByVersion migration.newVersion
		if (oldData.exists && !newData.exists) {
			INFO("migrating", oldData)
			migration migrate (oldData, newData)
		}
	}
	
	//------------------------------------------------------------------------------
	
	def trackFilesFor(file:File):TrackFiles	=
			TrackFiles(Storage metaDir file)
		
	def touch(tf:TrackFiles) {
		tf.meta.mkdirs()
		tf.meta setLastModifiedMilliInstant MilliInstant.now
		autoMigrate(tf)
	}
	
	def cleanup():Unit	= synchronized {
		INFO("cleaning library")
		
		val allTracks	= findTrackFiles()
		
		// how many to keep
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
				s"cleaned library",
				s"was ${info(allTracks)}",
				s"kept ${info(keepTracks)}",
				s"deleted ${info(deleteTracks)}"
			)
		}
		else {
			INFO(
				s"library untouched",
				s"was ${info(allTracks)}"
			)
		}
	}
	
	private def info(tracks:ISeq[TrackFiles]):String	= {
		val count	= tracks.size
		val space	= (tracks map spaceNeeded).sum
		(Human roundedBinary space) + "B for " +
		count + " " + (count == 1 cata ("tracks", "track"))
	}
	
	private def findTrackFiles():ISeq[TrackFiles]	= {
		def walk(dir:File):ISeq[TrackFiles]	=  {
			val candidate	= TrackFiles(dir)
				 if (interesting(candidate))	Vector(candidate)
			else if (dir.isDirectory)			dir.children.flattenMany flatMap walk
			else								Vector.empty
		}
		
		def interesting(it:TrackFiles):Boolean	=
				it.meta.isDirectory && {
					val standardFiles	=
							Set(
								it.wav,
								it.curve,
								it.data
							)
					val migrationFiles	=
							for {
								migration	<- Migration.all.toSet[Migration]
								version		<- Set(migration.oldVersion, migration.newVersion)
								dataFile	= it dataByVersion version
							}
							yield dataFile
					(standardFiles ++ migrationFiles) exists { _.isFile }
				}
				
		(walk(Storage.meta) sortBy lastModified).reverse
	}
			
	private def spaceNeeded(it:TrackFiles):Long	=
			it.wav.length	+
			it.curve.length
			
	private def lastModified(it:TrackFiles):MilliInstant	=
			it.meta.lastModifiedMilliInstant
}
