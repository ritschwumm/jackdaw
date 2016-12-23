name			:= "jackdaw"
organization	:= "de.djini"
version			:= "1.32.0"

scalaVersion	:= "2.12.0"
scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	"-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	// "-language:postfixOps",
	// "-language:experimental.macros"
	"-feature",
	// attention: requires build and runtime library version never differ
	"-opt:l:classpath",
	"-Ywarn-unused-import",
	"-Xfatal-warnings",
	"-Xlint"
)

conflictManager	:= ConflictManager.strict
libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil-core"	% "0.93.0"	% "compile",
	"de.djini"		%%	"scutil-swing"	% "0.93.0"	% "compile",
	"de.djini"		%%	"scaudio"		% "0.80.0"	% "compile",
	"de.djini"		%%	"scjson-io"		% "0.102.0"	% "compile",
	"de.djini"		%%	"screact"		% "0.101.0"	% "compile",
	"de.djini"		%%	"scgeom"		% "0.37.0"	% "compile",
	"de.djini"		%%	"sc2d"			% "0.28.0"	% "compile",
	"de.djini"					% "jkeyfinder"	% "0.3.0"	% "compile",
	"org.simplericity.macify"	% "macify"		% "1.6"		% "compile",
	"javazoom"					% "jlayer"		% "1.0.1"	% "compile",
	"com.mpatric"				% "mp3agic"		% "0.8.4"	% "compile",
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
	"-Xmx64m"
	// "-Xincgc"
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
osxappVm				:= OracleJava()
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
