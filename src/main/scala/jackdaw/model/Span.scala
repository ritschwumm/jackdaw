package jackdaw.model

final case class Span(start:Double, size:Double) {
	val end	= start + size
	
	def contains(frame:Double):Boolean	=
			frame >= start && frame < end
}
