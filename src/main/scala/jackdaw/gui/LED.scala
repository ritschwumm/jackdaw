package jackdaw.gui

object LED {
	enum Horizontal {
		case L, R
	}

	enum Vertical {
		case T, C, B
	}

	enum Point {
		case LT, RT, LC, RC, LB, RB
	}

	enum Segment {
		case HT, HC, HB, VLU, VRU, VLL, VRL
	}

	enum Number {
		case N0, N1, N2, N3, N4, N5, N6, N7, N8, N9
	}

	//------------------------------------------------------------------------------

	val pointPositions:Map[Point,(Horizontal,Vertical)]	={
		import Point.*
		import Horizontal.*
		import Vertical.*
		Map(
			LT	-> ((L,T)),
			RT	-> ((R,T)),
			LC	-> ((L,C)),
			RC	-> ((R,C)),
			LB	-> ((L,B)),
			RB	-> ((R,B))
		)
	}

	val segmentPoints:Map[Segment,(Point,Point)]	= {
		import Point.*
		import Segment.*
		Map(
			HT	-> (LT -> RT),
			HC	-> (LC -> RC),
			HB	-> (LB -> RB),
			VLU	-> (LT -> LC),
			VRU	-> (RT -> RC),
			VLL	-> (LC -> LB),
			VRL	-> (RC -> RB)
		)
	}

	val numberSegments:Map[Number,Set[Segment]]	= {
		import Number.*
		import Segment.*
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
}

