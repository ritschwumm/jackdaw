package jackdaw.data

final case class Span(start:Double, size:Double) {
	val end	= start + size
	
	def lock(value:Double):Double	=
			(value - start) % size + start
		
	def contains(value:Double):Boolean	=
			value >= start && value < end
		
	def move(offset:Double):Span	=
			copy(start = start + offset)
}
