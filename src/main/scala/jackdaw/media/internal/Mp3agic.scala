package jackdaw.media

import java.nio.file.*

import com.mpatric.mp3agic.*

import scutil.core.implicits.*
import scutil.lang.*
import scutil.log.*

import jackdaw.util.Checked

object Mp3agic extends Inspector with Logging {
	def name	= "Mp3agic"

	def readMetadata(input:Path):Checked[Metadata] =
		for {
			_		<-	recognizeFile(input)
			mp3file	<-	Catch.exception in new Mp3File(input.normalize.toString) leftMap { e =>
							ERROR("cannot create Mp3File", e)
							Checked problem1 "cannot create Mp3File"
						}
		}
		yield {
			val v1		= mp3file.hasId3v1Tag option mp3file.getId3v1Tag
			val title1	= v1 flatMap { _.getTitle.optionNotNull	}
			val artist1	= v1 flatMap { _.getArtist.optionNotNull	}
			val album1	= v1 flatMap { _.getAlbum.optionNotNull	}
			// val genre1	= v1 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }

			val v2		= mp3file.hasId3v2Tag option mp3file.getId3v2Tag
			val title2	= v2 flatMap { _.getTitle.optionNotNull	}
			val artist2	= v2 flatMap { _.getArtist.optionNotNull	}
			val album2	= v2 flatMap { _.getAlbum.optionNotNull	}
			// val genre2	= v2 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }

			Metadata(
				title	= title2	orElse title1,
				artist	= artist2	orElse artist1,
				album	= album2	orElse album1
				// genre	= genre2	orElse genre1
			)
		}

	private val recognizeFile:Path=>Checked[Unit]	=
		MediaUtil.requireFileSuffixIn(".mp3")
}
