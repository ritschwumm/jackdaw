package jackdaw.data

object Schema {
	// TODO hardcoded, look up references
	val default:Schema	= Schema(4, 4)
}

case class Schema(measuresPerPhrase:Int, beatsPerMeasure:Int) {
	val beatsPerPhrase:Int		= beatsPerMeasure * measuresPerPhrase
	
	/*
	def factor(from:RhythmUnit, to:RhythmUnit):Double	=
			(from, to) match {
				case (Phrase,	Beat)		=> 1.0 * beatsPerPhrase
				case (Phrase,	Measure)	=> 1.0 * measuresPerPhrase
				case (Phrase,	Phrase)		=> 1.0
				case (Measure,	Beat)		=> 1.0 * beatsPerMeasure
				case (Measure,	Measure)	=> 1.0
				case (Measure,	Phrase)		=> 1.0 / measuresPerPhrase
				case (Beat,		Beat)		=> 1.0
				case (Beat,		Measure)	=> 1.0 / beatsPerMeasure
				case (Beat,		Phrase)		=> 1.0 / beatsPerPhrase
			}
	*/
}
