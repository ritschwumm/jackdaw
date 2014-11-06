package jackdaw.media

import java.io.File

import com.mpatric.mp3agic._

import scutil.lang._
import scutil.implicits._
import scutil.log._

import jackdaw.audio.Metadata
import jackdaw.util.Checked

object Mp3agic extends Inspector with Logging {
	def name	= "Mp3agic"
	
	def readMetadata(input:File):Checked[Metadata] =
			for {
				_		<- recognizeFile(input)
				mp3file	<-
						Catch.exception in new Mp3File(input.getAbsolutePath) mapFail { e =>
							ERROR("cannot create Mp3File", e)
							Checked problem1 "cannot create Mp3File"
						}
						
				v1		= mp3file.hasId3v1Tag guard mp3file.getId3v1Tag
				title1	= v1 flatMap { _.getTitle.guardNotNull	}
				artist1	= v1 flatMap { _.getArtist.guardNotNull	}
				album1	= v1 flatMap { _.getAlbum.guardNotNull	}
				// genre1	= v1 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }
				
				v2		= mp3file.hasId3v2Tag guard mp3file.getId3v2Tag
				title2	= v2 flatMap { _.getTitle.guardNotNull	}
				artist2	= v2 flatMap { _.getArtist.guardNotNull	}
				album2	= v2 flatMap { _.getAlbum.guardNotNull	}
				// genre2	= v2 flatMap { _.getGenre into ID3v1Genres.GENRES.lift }
			}
			yield {
				Metadata(
					title	= title2	orElse title1, 
					artist	= artist2	orElse artist1,
					album	= album2	orElse album1
					// genre	= genre2	orElse genre1
				)
			}
			
	private val recognizeFile:File=>Checked[Unit]	=
			MediaUtil requireFileSuffixIn (".mp3")
}
