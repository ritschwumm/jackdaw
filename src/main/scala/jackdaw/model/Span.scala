package jackdaw.model

final case class Span(start:Double, size:Double) {
	val end	= start + size
	
	def lock(frame:Double):Double	=
			(frame - start) % size + start
		
	def contains(frame:Double):Boolean	=
			frame >= start && frame < end
		
	def move(offset:Double):Span	=
			copy(start = start + offset)
}
