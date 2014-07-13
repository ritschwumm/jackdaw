package djane.gui

object LED {
	sealed trait Horizontal
	case object L	extends Horizontal
	case object R	extends Horizontal
	
	sealed trait Vertical
	case object T	extends Vertical
	case object C	extends Vertical
	case object B	extends Vertical
	
	sealed trait Point
	case object LT	extends Point
	case object RT	extends Point
	case object LC	extends Point
	case object RC	extends Point
	case object LB	extends Point
	case object RB	extends Point
	
	sealed trait Segment
	case object HT	extends Segment
	case object HC	extends Segment
	case object HB	extends Segment
	case object VLU	extends Segment
	case object VRU	extends Segment
	case object VLL	extends Segment
	case object VRL	extends Segment
	
	sealed trait Number
	case object N0	extends Number
	case object N1	extends Number
	case object N2	extends Number
	case object N3	extends Number
	case object N4	extends Number
	case object N5	extends Number
	case object N6	extends Number
	case object N7	extends Number
	case object N8	extends Number
	case object N9	extends Number
	
	//------------------------------------------------------------------------------
	
	val pointPositions:Map[Point,(Horizontal,Vertical)]	=
			Map(
				LT	-> (L,T),
				RT	-> (R,T),
				LC	-> (L,C),
				RC	-> (R,C),
				LB	-> (L,B),
				RB	-> (R,B)
			)
	
	val segmentPoints:Map[Segment,(Point,Point)]	=
			Map(
				HT	-> (LT -> RT),
				HC	-> (LC -> RC),
				HB	-> (LB -> RB),
				VLU	-> (LT -> LC),
				VRU	-> (RT -> RC),
				VLL	-> (LC -> LB),
				VRL	-> (RC -> RB)
			)
			
	val numberSegments:Map[Number,Set[Segment]]	=
			Map(
				N0	-> Set(HT, HB,		VLU, VLL, VRU, VRL),
				N1	-> Set(				VRU, VRL),
				N2	-> Set(HT, HC, HB,	VRU, VLL),
				N3	-> Set(HT, HC, HB,	VRU, VRL),
				N4	-> Set(HC,			VLU, VRU, VRL),
				N5	-> Set(HT, HC, HB,	VLU, VRL),
				N6	-> Set(HT, HC, HB,	VLU, VLL, VRL),
				N7	-> Set(HT,			VRU, VRL),
				N8	-> Set(HT, HC, HB,	VLU, VLL, VRU, VRL),
				N9	-> Set(HT, HC, HB,	VLU, VRU, VRL)
			)
}

