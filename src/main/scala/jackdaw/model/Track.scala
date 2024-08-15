package jackdaw.model

import scala.annotation.nowarn

import java.nio.file.*

import scala.ref.WeakReference

import scutil.core.implicits.*
import scutil.gui.SwingUtil.*
import scutil.log.*
import scutil.io.*

import scaudio.sample.*

import screact.*

import jackdaw.Config
import jackdaw.library.*
import jackdaw.data.*
import jackdaw.media.*
import jackdaw.curve.*
import jackdaw.key.*
import jackdaw.persistence.*
import jackdaw.persistence.JsonProtocol.given

object Track extends Logging {
	private var cache:Map[Path,WeakReference[Track]]	= Map.empty

	private def insert(file:Path, track:Track):Unit	= {
		cache	=
			(cache filter { case (_, ref) => ref.get.isDefined }) +
			(file -> WeakReference(track))
	}

	// NOTE different Track instances for the same File would be fatal
	def load(file:Path):Option[Track]	=
		file.toRealPath() into { canon =>
			cache.get(canon).flatMap(_.get) `orElse` {
				loadImpl(canon) doto {
					_.foreach { track =>
						insert(canon, track)
					}
				}
			}
		}

	private def loadImpl(file:Path):Option[Track]	=
		try {
			Some(new Track(file).doto(_.load()))
		}
		catch { case e:Exception	=>
			ERROR(e)
			None
		}

	private val preferredFrameRate		= Config.outputConfig.rate
	private val preferredChannelCount	= 2
}

/** a loaded audio File with all its meta data */
final class Track private(val file:Path) extends Observing with Logging {
	val fileName	= file.getFileName.toString

	private val trackFiles	= Library.trackFilesFor(file)

	private val dataCell			= cell[TrackData](TrackData.empty)
	private val dataLoadedCell		= cell[Boolean](false)
	private val wavCell				= cell[Option[Path]](None)
	private val sampleCell			= cell[Option[Sample]](None)
	private val sampleLoadedCell	= cell[Boolean](false)
	private val bandCurveCell		= cell[Option[BandCurve]](None)
	private val fullyLoadedCell		= cell[Boolean](false)

	val data:Signal[TrackData]				= dataCell.signal
	val dataLoaded:Signal[Boolean]			= dataLoadedCell.signal
	val wav:Signal[Option[Path]]			= wavCell.signal
	val sample:Signal[Option[Sample]]		= sampleCell.signal
	val sampleLoaded:Signal[Boolean]		= sampleLoadedCell.signal
	val bandCurve:Signal[Option[BandCurve]]	= bandCurveCell.signal
	val fullyLoaded:Signal[Boolean]			= fullyLoadedCell.signal

	//------------------------------------------------------------------------------

	val annotation	= data.map(_.annotation)
	val cuePoints	= data.map(_.cuePoints)
	val rhythm		= data.map(_.rhythm)
	val metadata	= data.map(_.metadata	.map(_.data))
	val measure		= data.map(_.measure	.map(_.data))
	val key			= data.map(_.key		.map(_.data))

	//------------------------------------------------------------------------------

	@nowarn("msg=unused local definition")
	def setAnnotation(it:String):Unit	= {
		modifyData(TrackData.L.annotation set it)
	}

	def addCuePoint(nearFrame:Double, rhythmUnit:RhythmUnit):Unit	= {
		val snap:Double=>Double	=
			rhythm.current.cata(
				identity,
				rhythm => rhythm.raster(rhythmUnit).round(_)
			)
		modifyData {
			_.addCuePoint(snap(nearFrame))
		}
	}

	def removeCuePoint(nearFrame:Double):Unit	= {
		modifyData {
			_.removeCuePoint(nearFrame)
		}
	}

	def toogleRhythm(position:Double):Unit	= {
		updateRhythm(position, data.current.rhythm.isEmpty)
	}

	@nowarn("msg=unused local definition")
	private def updateRhythm(position:Double, activate:Boolean):Unit	= {
		val it	=
			activate.cata(
				None,
				detectedRhythm(position) `orElse` fakeRhythm(position)
			)
		modifyData(TrackData.L.rhythm set it)
	}

	private def fakeRhythm(position:Double):Option[Rhythm]	=
		sample.current.map { it =>
			Rhythm.fake(position, it.frameRate)
		}

	private def detectedRhythm(position:Double):Option[Rhythm]	=
		measure.current.map { measure =>
			Rhythm.default(position, measure)
		}

	def setRhythmAnchor(position:Double):Unit	= {
		modifyRhythm { _.copy(anchor=position) }
	}

	def moveRhythmBy(offset:Double):Unit	= {
		modifyRhythm { _.moveBy(offset) }
	}

	def resizeRhythmBy(factor:Double):Unit	= {
		modifyRhythm { _.resizeBy(factor) }
	}

	/** changes the rhythm size so lines under the cursor move with a constant offset */
	def resizeRhythmAt(position:Double, offset:Double):Unit	= {
		modifyRhythm { _.resizeAt(position, offset) }
	}

	/** does not modify if the rhythm would not be useful afterwards */
	@nowarn("msg=unused local definition")
	private def modifyRhythm(func:Rhythm=>Rhythm):Unit	= {
		modifyData(TrackData.L.rhythm mod { curr	=>
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
			valid `orElse` curr
		})
	}

	//------------------------------------------------------------------------------

	private val dataPersister	= new JsonPersister[TrackData]
	private val curvePersister	= new BandCurvePersister

	// NOTE using swing here is ugly
	@nowarn("msg=unused local definition")
	def load():Unit	= {
		worker {
			try {
				INFO("loading file", file)

				Library.touch(trackFiles)

				val fileModified	= MoreFiles.lastModified(file)

				// load cached data
				val dataVal:TrackData	=
					Files.exists(trackFiles.data)
					.flatOption {
						dataPersister.load(trackFiles.data)
					}
					.someEffect	{ _ =>
						INFO("using cached data")
					}
					.getOrElse {
						INFO("initializing data")
						TrackData.empty
					}
				edt { dataCell.set(dataVal) }()

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
						Inspector.readMetadata(file).map(Stamped(fileModified, _))
					}
					.noneEffect {
						WARN("cannot read metadata")
					}
				edt {
					modifyData(
						TrackData.L.metadata set metadataVal
					)
				}()

				// loaded enough to switch the track into the deck
				edt { dataLoadedCell.set(true) }()

				// provide wav
				// NOTE symlinks have the same last modified date as the link target,
				// otherwise it would make more sense to only check for a newer file
				val wavFresh:Boolean	=
					Files.exists(trackFiles.wav) &&
					MoreFiles.lastModified(trackFiles.wav) >= fileModified
				val wavVal:Option[Path]	=
					wavFresh.option(trackFiles.wav)
					.someEffect { _ =>
						INFO("using cached wav")
					}
					.orElse {
						INFO("decoding wav")
						val success	=
								Decoder.convertToWav(
									file,
									trackFiles.wav,
									Track.preferredFrameRate,
									Track.preferredChannelCount
								)
						success.option(trackFiles.wav)
					}
					.noneEffect {
						WARN("cannot decode wav")
					}
				edt { wavCell.set(wavVal) }()

				// provide sample
				val sampleVal:Option[Sample]	=
					wavVal.flatMap { wavFile =>
						INFO("getting sample")
						Wav.load(wavFile)
						.leftEffect	{	e =>
							ERROR("cannot get sample", e)
						}
						.toOption
					}
				edt { sampleCell.set(sampleVal) }()

				// loaded enough to actually play the track
				edt { sampleLoadedCell.set(true) }()

				// provide curve
				val curveFresh:Boolean	=
					Files.exists(trackFiles.curve) &&
					MoreFiles.lastModified(trackFiles.curve) >= MoreFiles.lastModified(trackFiles.wav)
				val curveVal:Option[BandCurve]	=
					curveFresh
					.flatOption	{
						curvePersister.load(trackFiles.curve)
					}
					.someEffect	{ _ =>
						INFO("using cached curve")
					}
					.orElse	{
						sampleVal.map { sampleVal =>
							INFO("calculating curve")
							BandCurve
							.calculate	(sampleVal, Config.curveRaster)
							.doto		(curvePersister.save(trackFiles.curve))
						}
					}
					.noneEffect {
						WARN("cannot provide curve")
					}
				edt { bandCurveCell.set(curveVal) }()

				// provide measure
				// TODO can use cached much earlier, before the curve is calculated!
				val measureVal:Option[Stamped[Double]]	=
					dataVal.measure
					.filter	{
						_.stamp >= fileModified
					}
					.someEffect	{ _ =>
						INFO("using cached beat rate")
					}
					.orElse {
						curveVal.map { curveVal =>
							INFO("detecting beat rate")
							val out:Double	=
								MeasureDetector.measureFrames(
									curveVal,
									Config.detectBpsRange,
									Schema.default.beatsPerMeasure
								)
							Stamped(fileModified, out)
						}
					}
					.noneEffect {
						WARN("cannot provide beat rate")
					}
				edt {
					modifyData(
						TrackData.L.measure set measureVal
					)
				}()

				// provide key
				val keyVal:Option[Stamped[MusicKey]]	=
					dataVal.key
					.filter	{
						_.stamp >= fileModified
					}
					.someEffect	{ _ =>
						INFO("using cached key")
					}
					.orElse {
						sampleVal.map { sampleVal =>
							INFO("detecting key")
							val out:MusicKey	= KeyDetector.findKey(sampleVal)
							Stamped(fileModified, out)
						}
					}
					.noneEffect {
						WARN("cannot provide key")
					}
				edt {
					modifyData(
						TrackData.L.key set keyVal
					)
				}()

				INFO("track loaded")
				edt { fullyLoadedCell.set(true) }()
			}
			catch { case e:Exception =>
				ERROR("track could not be loaded", e)
				edt { dataLoadedCell.set(false) }()
				edt { fullyLoadedCell.set(false) }()
			}
		}
	}

	private def modifyData(func:TrackData=>TrackData):Unit	= {
		val modified	= func(dataCell.current)
		dataCell.set(modified)
		dataPersister.save(trackFiles.data)(modified)
	}
}
