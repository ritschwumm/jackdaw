package jackdaw.model

import java.io.File

import scutil.lang._
import scutil.implicits._
import scutil.text.Human
import scutil.log._
import scutil.time._

import jackdaw.Config

object Library extends Logging {
	/*
	def main(args:Array[String]) {
		findTrackFiles zipBy { _ |> spaceNeeded |> humanize } foreach println
	}
	*/

	//------------------------------------------------------------------------------
	
	var allTracks:Synchronized[ISeq[TrackFiles]]	= Synchronized(Vector.empty)
	
	def init() {
		// newest first
		allTracks set (findTrackFiles sortBy lastModified).reverse
	}
	
	def trackFilesFor(file:File):TrackFiles	=
			TrackFiles(Storage metaDir file)
		
	def insert(tf:TrackFiles) {
		tf.meta.mkdirs() 
		tf.meta updateLastModifiedMilliInstant MilliInstant.now
				
		allTracks modify { original =>
			INFO("adding track")
			
			// insert track, newest first
			(original  filterNot { _.meta ==== tf.meta }) prepend tf
		}
	}
	
	def cleanup() {
		allTracks modify { allTracks =>
			INFO("cleaning library")
			
			// how many to keep
			val (keepTracks, deleteTracks)	= {
				val needs	= allTracks map spaceNeeded
				val sums	= (needs scanLeft 0L) { _+_ }
				val keeps	= sums.tail takeWhile { _ < Config.maxCacheSize }
				val pos		= keeps.size max Config.minCacheCount
				allTracks splitAt pos
			}
			
			val allInfo		= info(allTracks)
			val keepInfo	= info(keepTracks)
			val deleteInfo	= info(deleteTracks)
			
			deleteTracks foreach removeCache
			
			// inform user
			if (deleteInfo.count != 0) {
				INFO(
					s"cleaned library",
					s"was ${allInfo.human}",
					s"kept ${keepInfo.human}",
					s"deleted ${deleteInfo.human}"
				)
			}
			else {
				INFO(
					s"library untouched",
					s"was ${allInfo.human}"
				)
			}
			
			keepTracks
		}
	}
	
	case class Info(count:Int, space:Long) {
		def human:String	=
				(Human roundedBinary space) + "B for " + 
				count + " " + (count == 1 cata ("tracks", "track"))
	}
	
	def info(tracks:ISeq[TrackFiles]):Info	=
			Info(
				tracks.size, 
				(tracks map spaceNeeded).sum
			)
	
	private def findTrackFiles:ISeq[TrackFiles]	= {
		def walk(dir:File):ISeq[TrackFiles]	=  {
			val candidate	= TrackFiles(dir)
			if (interesting(candidate))	Vector(candidate)
			else if (dir.isDirectory)	dir.children.flattenMany flatMap walk
			else						Vector.empty
		}
		walk(Storage.meta)
	}
	
	private def interesting(it:TrackFiles):Boolean	=
			it.meta.isDirectory && (it.wav.isFile || it.curve.isFile)
			
	private def spaceNeeded(it:TrackFiles):Long	=
			it.wav.length()		+
			it.curve.length()
			
	private def lastModified(it:TrackFiles):MilliInstant	=
			it.meta.lastModifiedMilliInstant
		
	private def removeCache(it:TrackFiles) {
		it.wav.delete()	
		it.curve.delete()
	}
}
