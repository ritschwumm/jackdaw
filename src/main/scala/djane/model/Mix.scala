package djane.model

/** complete mixer state */
final class Mix {
	val master	= Strip.forMaster
	val strip1	= Strip.forDeck
	val strip2	= Strip.forDeck
	val strip3	= Strip.forDeck
	val tone1	= new Tone
	val tone2	= new Tone
	val tone3	= new Tone
}
