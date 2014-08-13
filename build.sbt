name			:= "djane"

organization	:= "de.djini"

version			:= "1.1.0"

scalaVersion	:= "2.11.2"

libraryDependencies	++= Seq(
	"de.djini"					%% "scutil-core"	% "0.48.0"	% "compile",
	"de.djini"					%% "scutil-swing"	% "0.48.0"	% "compile",
	"de.djini"					%% "scaudio"		% "0.33.0"	% "compile",
	"de.djini"					%% "scjson"			% "0.53.0"	% "compile",
	"de.djini"					%% "screact"		% "0.54.0"	% "compile",
	"de.djini"					%% "scgeom"			% "0.22.0"	% "compile",
	"de.djini"					%% "sc2d"			% "0.16.0"	% "compile",
	"org.simplericity.macify"	%	"macify"		% "1.6"		% "compile"
)

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
	"-optimize"
)

//------------------------------------------------------------------------------

buildInfoSettings

sourceGenerators in Compile	<+= buildInfo

buildInfoKeys		:= Seq[BuildInfoKey](name, version)	// name, version, scalaVersion, sbtVersion

buildInfoPackage	:= "djane"

//--------------------------------------------------------------------------------

scriptstartSettings

scriptstartConfigs	:= Seq(ScriptConfig(
	scriptName	= "djane",
	mainClass	= "djane.DJane",
	// @see http://download.oracle.com/javase/1.5.0/docs/guide/2d/flags.html
	// -Dsun.java2d.trace=log
	vmArguments	= Seq(
		"-server",  
		"-Xms48m",
		"-Xmx128m",
	//	"-Xincgc",
		"-XX:+UnlockExperimentalVMOptions",
		"-XX:+UseG1GC",
	//	"-XX:+PrintGCDetails",
		//
		// java.lang.IllegalArgumentException: Comparison method violates its general contract!
		// 	at java.util.TimSort.mergeHi(TimSort.java:868)
		//	...
		//	at sun.awt.datatransfer.DataTransferer.setToSortedDataFlavorArray(DataTransferer.java:2407)
		// @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7193557
		// @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7173464
		"-Djava.util.Arrays.useLegacyMergeSort=true",
		//	prevents memleaks for windows
		"-Dswing.bufferPerWindow=false"
	//
	//	new xrender pipeline
	//	"-Dsun.java2d.xrender=True"
	//
	// crashes on too many systems, fbobject=false helps
	//	"-Dsun.java2d.opengl=True",
	//	"-Dsun.java2d.opengl.fbobject=false"
	//
	// allows jvisualvm
	//	"-Dcom.sun.management.jmxremote.local.only=false"
	)
))

// scriptstart::zipper
inTask(scriptstart)(zipperSettings ++ Seq(
	zipperFiles	:= selectSubpaths(scriptstart.value, -DirectoryFilter).toSeq
))

//------------------------------------------------------------------------------

osxappSettings

osxappBundleName	:= "djane"

osxappBundleIcons	:= baseDirectory.value / "src/main/osxapp/default.icns"

osxappVm			:= OracleJava7()	// AppleJava6()

osxappMainClass		:= Some("djane.DJane")

osxappVmOptions		:= Seq(
	"-server",
	"-Xms48m",
	"-Xmx512m",
	"-Xincgc"
)

osxappProperties	:= Map(
	// fix dnd exception
	"java.util.Arrays.useLegacyMergeSort"	-> "true",
	//	prevents memleaks for windows
	"swing.bufferPerWindow"					-> "false"
)

// osxappApplicationStub	:= baseDirectory.value / "src/osxapp/resources/JavaApplicationStub"

// osxapp::zipper
inTask(osxapp)(zipperSettings ++ Seq(
	zipperFiles		:= selectSubpaths(osxapp.value, -DirectoryFilter).toSeq,
	zipperBundle	:= zipperBundle.value + ".app" 
))

//------------------------------------------------------------------------------

// build bundles in target/../zipper
TaskKey[Seq[File]]("bundle")	:= Seq(
	(zipper in scriptstart).value,
	(zipper in osxapp).value
)
