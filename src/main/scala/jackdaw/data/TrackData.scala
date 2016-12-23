package jackdaw.data

import scala.math._

import scutil.base.implicits._
import scutil.lang._

import jackdaw.media.Metadata
import jackdaw.key.MusicKey

object TrackData {
	val empty	=
			TrackData(
				annotation	= "",
				cuePoints	= ISeq.empty,
				rhythm		= None,
				metadata	= None,
				measure		= None,
				key			= None
			)
			
	val L	= Lenser[TrackData]
}

final case class TrackData(
	annotation:String,
	cuePoints:ISeq[Double],
	rhythm:Option[Rhythm],
	metadata:Option[Stamped[Metadata]],
	measure:Option[Stamped[Double]],
	key:Option[Stamped[MusicKey]]
) {
	// BETTER make this a class
	
	def addCuePoint(frame:Double):TrackData	=
			copy(cuePoints = (cuePoints :+ frame).sorted)
		
	def removeCuePoint(nearFrame:Double):TrackData	=
			(	for {
					index		<- nearestCuePointIndex(nearFrame)
					cuePoints	<- cuePoints removeAt index
				}
				yield copy(cuePoints = cuePoints)
			)
			.getOrElse	(this)
			
	private def nearestCuePointIndex(nearFrame:Double):Option[Int]	=
			cuePoints
			.map	{ frame => abs(frame - nearFrame) }
			.zipWithIndex
			.sortBy	{ _._1 }
			.headOption
			.map	{ _._2 }
}
