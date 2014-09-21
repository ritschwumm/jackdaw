package jackdaw.util

object Math {
	@inline
	def max3(a:Float, b:Float, c:Float):Float	=
			if (a > b) {
				if (a > c)	a
				else		c
			}
			else {
				if (b > c)	b
				else		c
			}
}

