package jackdaw

import java.awt._
import javax.swing._

import org.simplericity.macify.eawt._

import scutil.jdk.implicits._
import scutil.lang._
import scutil.gui.instances._
import scutil.gui.CasterInstances._

import screact._
import screact.swing._

import jackdaw.library._
import jackdaw.model._
import jackdaw.gui._
import jackdaw.gui.util.Keyboard
import jackdaw.remote.EngineStub

/** main class initializing backend and gui */
object Main extends Observing {
	def create(shutdown:Io[Unit]):IoResource[Unit]	=
		for {
			_				<-	IoResource delay { Style.setupLnF() }
			_				<-	IoResource delay { Library.init() }
			windowActive	<-	IoResource delay cell(false)
			engine			<-	EngineStub.create
			model			<-	Model.create(engine)
			keyboard		<-	Keyboard.create
			ui				<-	IoResource delay new MainUI(model, keyboard, windowActive)
			frame			<-	IoResource.unsafe releasable new JFrame
		}
		yield {
			val windowActiveFb:Events[Boolean]	=
				SwingWidget
				.events	((frame:WindowCaster).connect)
				.map	{ _.getWindow.isActive }

			windowActiveFb observe windowActive.set

			val content	= frame.getContentPane
			content setLayout new BorderLayout
			content.add(ui.component, BorderLayout.CENTER)
			frame setTitle					Style.window.title
			frame setIconImage				Style.window.icon
			frame setSize					Style.window.size
			frame setDefaultCloseOperation	WindowConstants.DO_NOTHING_ON_CLOSE
			frame onWindowClosing { _ => shutdown.unsafeRun() }
			frame setVisible true

			// keep a strong reference to the MainUI so it doesn't
			// get collected and looses reactivity
			ui.component.putClientProperty("STRONG_REF", ui)

			val macifyApplication	= new DefaultApplication
			macifyApplication addApplicationListener new ApplicationAdapter {
				override def handleQuit(ev:ApplicationEvent):Unit	= shutdown.unsafeRun()
			}
			macifyApplication setApplicationIconImage Style.application.osxIcon
			macifyApplication.removeAboutMenuItem()
			macifyApplication.removePreferencesMenuItem()

			model.speed persist (Config.dataBase / "speed.json")
		}
}
