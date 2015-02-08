package jackdaw.model

import java.io.File

import scutil.lang._
import scutil.implicits._
import scutil.gui.SwingUtil._
import scutil.log._

import scaudio.sample._

import screact._

import jackdaw.Config
import jackdaw.audio._
import jackdaw.data._
import jackdaw.media._
import jackdaw.persistence._
import jackdaw.persistence.JSONProtocol._
import jackdaw.util.LRU

/** a loaded audio File with all its meta data */
object Track extends Logging {
	private val lru	=
			new LRU[File,Track](
				Config.curTrackCount,
				new Track(_),
				_.load()
			)
			
	def load(file:File):Option[Track]	= 
			try {
				// NOTE different Track objects for the same File would be fatal
				Some(lru load file.getCanonicalFile)
			}
			catch { case e:Exception	=> 
				ERROR(e)
				None
			}
	
	def dispose() {
		lru.dispose()
	}
			
	private val preferredFrameRate		= Config.outputConfig.rate
	private val preferredChannelCount	= 2
}

final class Track(val file:File) extends Observing with Logging {
	val fileName	= file.getName
	
	private val trackFiles	= Library trackFilesFor file
	Library insert trackFiles
	Library cleanup ()
	
	private val dataCell			= cell[TrackData](TrackData.empty)
	private val dataLoadedCell		= cell[Boolean](false)
	private val wavCell				= cell[Option[File]](None)
	private val sampleCell			= cell[Option[Sample]](None)
	private val sampleLoadedCell	= cell[Boolean](false)
	private val bandCurveCell		= cell[Option[BandCurve]](None)
	private val fullyLoadedCell		= cell[Boolean](false)
	
	val data:Signal[TrackData]				= dataCell.signal
	val dataLoaded:Signal[Boolean]			= dataLoadedCell.signal
	val wav:Signal[Option[File]]			= wavCell.signal
	val sample:Signal[Option[Sample]]		= sampleCell.signal
	val sampleLoaded:Signal[Boolean]		= sampleLoadedCell.signal
	val bandCurve:Signal[Option[BandCurve]]	= bandCurveCell.signal
	val fullyLoaded:Signal[Boolean]			= fullyLoadedCell.signal
	
	private def modifyData(func:Endo[TrackData]) {
		val modified	= func(dataCell.current)
		dataCell set modified
		(dataPersister save trackFiles.data)(modified)
	}
	
	//------------------------------------------------------------------------------
	
	private val dataPersister	= new JSONPersister[TrackData]
	private val curvePersister	= new BandCurvePersister
	
	// NOTE using swing here is ugly
	def load() {
		worker {
			try {
				INFO("loading file", file)
				
				val fileModified	= file.lastModifiedMilliInstant
				
				// load cached data
				val dataVal:TrackData	=
						trackFiles.data.exists
						.flatGuard {
							dataPersister load trackFiles.data
						}
						.someEffect	{ _ =>
							INFO("using cached data")
						}
						.getOrElse {
							INFO("initializing data")
							TrackData.empty
						}
				edtWait { dataCell set dataVal }
				
				// provide metadata
				val metadataVal:Option[Stamped[Metadata]]	=
						dataVal.metadata
						.filter	{
							_.stamp >= fileModified 
						}
						.someEffect { _ =>
							INFO("using cached metadata") 
						}
						.orElse {
							INFO("reading metadata")
							Inspector readMetadata file map { Stamped(fileModified, _) }
						}
						.noneEffect {
							WARN("cannot read metadata")
						}
				edtWait {
					modifyData(
						TrackData.L.metadata putter metadataVal
					)
				}
				
				// loaded enough to switch the track into the deck
				edtWait { dataLoadedCell set true }
				
				// provide wav
				// NOTE symlinks have the same last modified date as the link target,
				// otherwise it would make more sense to only check for a newer file
				val wavFresh:Boolean	= 
						trackFiles.wav.exists && 
						trackFiles.wav.lastModifiedMilliInstant >= fileModified
				val wavVal:Option[File]	=
						(wavFresh guard trackFiles.wav)
						.someEffect { _ =>
							INFO("using cached wav")
						}
						.orElse {
							INFO("decoding wav")
							val success	=
									Decoder convertToWav (
										file,
										trackFiles.wav,
										Track.preferredFrameRate,
										Track.preferredChannelCount
									)
							success guard trackFiles.wav
						}
						.noneEffect {
							WARN("cannot decode wav")
						}
				edtWait { wavCell set wavVal }
				
				// provide sample
				val sampleVal:Option[Sample]	=
						wavVal flatMap { wavFile =>
							INFO("getting sample")
							(Wav load wavFile)
							.failEffect	{	e =>
								ERROR("cannot get sample", e)
							}
							.toOption
						}
				edtWait { sampleCell set sampleVal }
				
				// loaded enough to actually play the track
				edtWait { sampleLoadedCell set true }
				
				// provide curve
				val curveFresh:Boolean	= 
						trackFiles.curve.exists &&
						trackFiles.curve.lastModifiedMilliInstant >= trackFiles.wav.lastModifiedMilliInstant
				val curveVal:Option[BandCurve]	=
						curveFresh
						.flatGuard	{
							curvePersister load trackFiles.curve
						}
						.someEffect	{ _ =>
							INFO("using cached curve")
						}
						.orElse	{
							sampleVal map { sampleVal =>
								INFO("calculating curve")
								BandCurve
								.calculate	(sampleVal, Config.curveRaster) 
								.doto		(curvePersister save trackFiles.curve)
							}
						}
						.noneEffect {
							WARN("cannot provide curve")
						}
				edtWait { bandCurveCell set curveVal }
				
				// provide measure
				val measureVal:Option[Stamped[Double]]	=
						dataVal.measure
						.filter	{
							_.stamp >= fileModified
						}
						.someEffect	{ _ =>
							INFO("using cached beat rate")
						}
						.orElse {
							curveVal map { curveVal =>
								INFO("detecting beat rate")
								val out	=
										MeasureDetector measureFrames (
											curveVal, 
											Config.detectBpsRange,
											Rhythm.defaultBeatsPerMeasure
										)
								Stamped(fileModified, out)
							}
						}
						.noneEffect {
							WARN("cannot provide beat rate")
						}
				edtWait {
					modifyData(
						TrackData.L.measure putter measureVal
					)
				}
				
				INFO("track loaded")
				edtWait { fullyLoadedCell set true }
			}
			catch { case e:Exception =>
				ERROR("track could not be loaded", e)
				edtWait { dataLoadedCell	set false }
				edtWait { fullyLoadedCell	set false }
			}
		}
	}
	
	//------------------------------------------------------------------------------
	
	val annotation	= data map { _.annotation	}
	val cuePoints	= data map { _.cuePoints	}
	val rhythm		= data map { _.raster		}
	val metadata	= data map { _.metadata	map { _.data	} }
	val measure		= data map { _.measure	map { _.data	} }
	
	//------------------------------------------------------------------------------
	
	def setAnnotation(it:String) {
		modifyData(TrackData.L.annotation putter it)
	}
	
	def addCuePoint(nearFrame:Double, rhythmUnit:RhythmUnit) {
		val snap:Endo[Double]	=
				rhythm.current cata (
					identity,
					rhythm => rhythm raster rhythmUnit round _
				)
		modifyData {
			_ addCuePoint snap(nearFrame)
		}
	}
	
	def removeCuePoint(nearFrame:Double) {
		modifyData {
			_ removeCuePoint nearFrame
		}
	}
	
	def toogleRhythm(position:Double) {
		updateRhythm(position, data.current.raster.isEmpty)
	}
	
	private def updateRhythm(position:Double, activate:Boolean) {
		val it	=
				activate cata (
					None,
					detectedRhythm(position) orElse fakeRhythm(position)
				)
		modifyData(TrackData.L.raster putter it)
	}
	
	private def fakeRhythm(position:Double):Option[Rhythm]	= 
			sample.current map { it => 
				Rhythm fake (position, it.frameRate) 
			}
			
	private def detectedRhythm(position:Double):Option[Rhythm]	= 
			measure.current map { measure =>
				Rhythm default (position, measure)
			}
	
	def setRhythmAnchor(position:Double) {
		modifyRhythm { _ copy (anchor=position) }
	}
	
	def moveRhythmBy(offset:Double) {
		modifyRhythm { _ moveBy offset }
	}
	
	def resizeRhythmBy(factor:Double) {
		modifyRhythm { _ resizeBy factor }
	}
	
	/** changes the rhythm size so lines under the cursor move with a constant offset */
	def resizeRhythmAt(position:Double, offset:Double) {
		modifyRhythm { _ resizeAt (position, offset) }
	}
	
	/** does not modify if the rhythm would not be useful afterwards */
	private def modifyRhythm(func:Rhythm=>Rhythm) {
		modifyData(TrackData.L.raster modifier { curr	=>
			val valid	=
					for {
						base	<- curr
						rhythm	= func(base)
						sample	<- sample.current
						rate	= sample.frameRate
						beat	= rhythm.beat
						if	beat >= rate / Config.rhythmBpsRange._2 &&
							beat <= rate / Config.rhythmBpsRange._1
					}
					yield rhythm
			valid orElse curr
		})
	}
	
	//------------------------------------------------------------------------------
	
	// initialize
	load()
}
