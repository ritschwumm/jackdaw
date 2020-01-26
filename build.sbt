name			:= "jackdaw"
organization	:= "de.djini"
version			:= "1.38.0"

scalaVersion	:= "2.13.1"
scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	"-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	// "-language:experimental.macros"
	"-feature",
	// attention: requires build and runtime library version never differ
	"-opt:l:inline",
	"-opt-inline-from:**",
	"-Xfatal-warnings",
	"-Xlint"
)

conflictManager		:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js)(\\..*)?)$"
libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil-core"		% "0.170.0"	% "compile",
	"de.djini"		%%	"scutil-swing"		% "0.170.0"	% "compile",
	"de.djini"		%%	"scaudio"			% "0.161.0"	% "compile",
	"de.djini"		%%	"scjson-io-pickle"	% "0.189.0"	% "compile",
	"de.djini"		%%	"screact"			% "0.182.0"	% "compile",
	"de.djini"		%%	"scgeom"			% "0.44.0"	% "compile",
	"de.djini"		%%	"sc2d"				% "0.33.0"	% "compile",
	"de.djini"					% "jkeyfinder"	% "0.4.1"	% "compile",
	"org.simplericity.macify"	% "macify"		% "1.6"		% "compile",
	"javazoom"					% "jlayer"		% "1.0.1"	% "compile",
	"com.mpatric"				% "mp3agic"		% "0.9.1"	% "compile",
	"de.jarnbjo"				% "j-ogg-all"	% "1.0.0"	% "compile"
)

wartremoverErrors ++= Seq(
	Wart.StringPlusAny,
	Wart.EitherProjectionPartial,
	Wart.OptionPartial,
	Wart.Enumeration,
	Wart.FinalCaseClass,
	Wart.JavaConversions,
	Wart.Option2Iterable,
	Wart.TryPartial
)

enablePlugins(BuildInfoPlugin, OsxAppPlugin, CapsulePlugin, ScriptStartPlugin)

//------------------------------------------------------------------------------

buildInfoKeys		:= Seq[BuildInfoKey](name, version)
buildInfoPackage	:= "jackdaw"

//--------------------------------------------------------------------------------

val bootClass	= "jackdaw.Boot"

val vmOptions	= Seq(
	"-server",
	"-Xms64m",
	"-Xmx64m",
	"-XX:+UseG1GC",
	// switch off access to /tmp/hsperfdata_$USER
	"-XX:-UsePerfData",
	"-XX:+PerfDisableSharedMem"
	// full (mixed?) collections in G1 take far too long
	// "-XX:+UnlockExperimentalVMOptions",
	// "-XX:+UseG1GC",
	// "-XX:MaxGCPauseMillis=10"
	// "-XX:+PrintGCApplicationStoppedTime"
	// "-XX:+PrintGCDetails",
)
val systemProperties	= Map(
	// java.lang.IllegalArgumentException: Comparison method violates its general contract!
	// 	at java.util.TimSort.mergeHi(TimSort.java:868)
	//	...
	//	at sun.awt.datatransfer.DataTransferer.setToSortedDataFlavorArray(DataTransferer.java:2407)
	// @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7193557
	// @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7173464
	"java.util.Arrays.useLegacyMergeSort"	-> "true",

	//	prevents memleaks for windows
	"swing.bufferPerWindow"					-> "false"

	//	debug rendering
	//		@see http://download.oracle.com/javase/1.5.0/docs/guide/2d/flags.html
	//		"sun.java2d.trace"	-> "log"

	//	new xrender pipeline
	//		"sun.java2d.xrender"	-> "True"

	//	crashes on too many systems, fbobject=false helps
	//		"sun.java2d.opengl"	-> "True",
	//		"sun.java2d.opengl.fbobject"	-> "false"

	//	allows jvisualvm
	//		"com.sun.management.jmxremote.local.only"	-> "false"
)

// osxappBundleName		:= name.value
osxappBundleIcons		:= baseDirectory.value / "src/main/osxapp/default.icns"
osxappVm				:= JavaHomeVersion("1.8+")
osxappMainClass			:= Some(bootClass)
osxappVmOptions			:= vmOptions
osxappSystemProperties	:= systemProperties

capsuleMainClass		:= Some(bootClass)
capsuleVmOptions		:= vmOptions
capsuleSystemProperties	:= systemProperties
capsuleMinJavaVersion	:= Some("1.8.0")
capsuleMakeExecutable	:= true

scriptstartConfigs	:= Seq(ScriptConfig(
	scriptName			= "jackdaw",
	mainClass			= bootClass,
	vmOptions			= vmOptions,
	systemProperties	= systemProperties
))

TaskKey[Seq[File]]("bundle")	:= Seq(
	osxappZip.value,
	capsule.value,
	scriptstart.value
)
