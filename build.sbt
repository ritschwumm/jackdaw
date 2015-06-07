name			:= "jackdaw"
organization	:= "de.djini"
version			:= "1.26.0"

scalaVersion	:= "2.11.6"
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
	"-optimize",
	"-Ywarn-unused-import"
	// "-Yinline-warnings",
	// "-Xfatal-warnings"
)

conflictManager	:= ConflictManager.strict
libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil-core"	% "0.68.0"	% "compile",
	"de.djini"		%%	"scutil-swing"	% "0.68.0"	% "compile",
	"de.djini"		%%	"scaudio"		% "0.55.0"	% "compile",
	"de.djini"		%%	"scjson"		% "0.73.0"	% "compile",
	"de.djini"		%%	"screact"		% "0.76.0"	% "compile",
	"de.djini"		%%	"scgeom"		% "0.28.0"	% "compile",
	"de.djini"		%%	"sc2d"			% "0.21.0"	% "compile",
	"com.twitter"	%%	"chill"			% "0.6.0"	% "compile",
	"de.djini"					% "jkeyfinder"	% "0.2.0"	% "compile",
	"org.simplericity.macify"	% "macify"		% "1.6"		% "compile",
	"javazoom"					% "jlayer"		% "1.0.1"	% "compile",
	"com.mpatric"				% "mp3agic"		% "0.8.3"	% "compile",
	"de.jarnbjo"				% "j-ogg-all"	% "1.0.0"	% "compile"
	// "com.esotericsoftware"		% "kryo"		% "3.0.0"	% "compile",
)
dependencyOverrides	++= Set(
	"org.scala-lang"	% "scala-library"	% scalaVersion.value,
	"org.scala-lang"	% "scala-reflect"	% scalaVersion.value
)

enablePlugins(ScriptStartPlugin, OsxAppPlugin, CapsulePlugin)

//------------------------------------------------------------------------------

buildInfoSettings
sourceGenerators in Compile	<+= buildInfo
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
	//		"-Dsun.java2d.xrender=True"
	
	//	crashes on too many systems, fbobject=false helps
	//		"-Dsun.java2d.opengl=True",
	//		"-Dsun.java2d.opengl.fbobject=false"
	
	//	allows jvisualvm
	//		"-Dcom.sun.management.jmxremote.local.only=false"
)

// val mainClassX		= Keys.mainClass.value.get	// "jackdaw.Boot"
	
scriptstartConfigs	:= Seq(ScriptConfig(
	scriptName			= name.value,
	mainClass			= bootClass,
	vmOptions			= vmOptions,
	systemProperties	= systemProperties
))

// osxappBundleName		:= name.value
osxappBundleIcons		:= baseDirectory.value / "src/main/osxapp/default.icns"
osxappVm				:= OracleJava7()
osxappMainClass			:= Some(bootClass)
osxappVmOptions			:= vmOptions
osxappSystemProperties	:= systemProperties

capsuleMainClass		:= Some(bootClass)
capsuleVmOptions		:= vmOptions
capsuleSystemProperties	:= systemProperties
capsuleMinJavaVersion	:= Some("1.7.0")
capsuleMakeExecutable	:= true

TaskKey[Seq[File]]("bundle")	:= Seq(
	scriptstartZip.value,
	osxappZip.value,
	capsule.value
)
