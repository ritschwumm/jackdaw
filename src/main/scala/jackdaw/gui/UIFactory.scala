package jackdaw.gui

import screact._

import jackdaw.range._

object UIFactory {
	//------------------------------------------------------------------------------
	//## rotary
	
	def filterRotary(value:Signal[Double])	=
			new RotaryUI(
				value	= value,
				FilterRange.min,
				FilterRange.max,
				FilterRange.neutral
			)
			
	def trimRotary(value:Signal[Double])	=
			new RotaryUI(
				value	= value,
				TrimRange.min,
				TrimRange.max,
				TrimRange.neutral
			)
	
	def volumeRotary(value:Signal[Double])	=
			new RotaryUI(
				value	= value,
				VolumeRange.min,
				VolumeRange.max,
				VolumeRange.min
			)
	
	//------------------------------------------------------------------------------
	//## linear
	
	def volumeLinear(value:Signal[Double]) =
			new LinearUI(
				value		= value,
				minimum		= VolumeRange.min,
				maximum		= VolumeRange.max,
				neutral		= Some(VolumeRange.min),
				vertical	= true
			)
	
	def pitchLinear(value:Signal[Double])	=
			new LinearUI(
				value		= value,
				minimum		= PitchRange.min,
				maximum		= PitchRange.max,
				neutral		= Some(PitchRange.neutral),
				vertical	= true
			)
	
	def speedLinear(value:Signal[Double])	=
			new LinearUI(
				value		= value,
				minimum		= SpeedRange.min,
				maximum		= SpeedRange.max,
				neutral		= None,
				vertical	= false
			)
	
	//------------------------------------------------------------------------------
	//## meter
	
	def meter(value:Signal[Float], range:MeterRange):MeterUI		=
			new MeterUI(value, range, true)
}
