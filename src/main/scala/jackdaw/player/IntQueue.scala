package jackdaw.player

final class IntQueue(maxSize:Int) {
	private var items:Array[Int]	= new Array[Int](maxSize)
	private var curSize:Int			= 0
	
	def size:Int 		= curSize
	
	def full:Boolean	= curSize >= maxSize
	
	def shift():Int	= {
		val out	= items(0)
		var ptr	= 0
		while (ptr < curSize-1) {
			items(ptr)	= items(ptr+1)
			ptr	+= 1
		}
		curSize	= ptr
		out
	}
	
	def push(item:Int) {
		items(curSize)	= item
		curSize		+= 1
	}
	
	def removeEqual(toRemove:Int) {
		var inPtr	= 0
		var outPtr	= 0
		while (inPtr < curSize) {
			val item	= items(inPtr)
			if (item != toRemove) {
				items(outPtr)	= item
				outPtr	+= 1
			}
			inPtr	+= 1
		}
		curSize	= outPtr
	}
}
