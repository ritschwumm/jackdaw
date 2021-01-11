package jackdaw.data

import scala.math._

import scutil.core.implicits._
import scutil.lang._

import jackdaw.media.Metadata
import jackdaw.key.MusicKey

object TrackData {
	val empty	=
		TrackData(
			annotation	= "",
			cuePoints	= Seq.empty,
			rhythm		= None,
			metadata	= None,
			measure		= None,
			key			= None
		)

	val L	= Lens.Gen[TrackData]
}

final case class TrackData(
	annotation:String,
	cuePoints:Seq[Double],
	rhythm:Option[Rhythm],
	metadata:Option[Stamped[Metadata]],
	measure:Option[Stamped[Double]],
	key:Option[Stamped[MusicKey]]
) {
	// BETTER make this a class

	def addCuePoint(frame:Double):TrackData	=
		copy(cuePoints = (cuePoints :+ frame).sorted(Ordering.Double.TotalOrdering))

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
		.sortBy	{ _._1 }(Ordering.Double.TotalOrdering)
		.headOption
		.map	{ _._2 }
}
