package djane

import java.io.File
import java.awt._
import java.awt.event._
import javax.swing._

import org.simplericity.macify.eawt._

import scutil.implicits._
import scutil.log.Logging
import scutil.gui.CasterInstances._

import screact._
import screact.swing._

import djane.model._
import djane.gui._

/** main class initializing backend and gui */
object DJaneMain extends Observing {
	Style.setupLnF()
	
	private val windowActive	= cell(false)
	
	private val	model	= new Model
	private val ui		= new DJaneUI(model, windowActive)
	private val	frame	= new JFrame
	
	private val windowActiveFb:Events[Boolean]	=
			SwingWidget 
			.events	((frame:WindowCaster).connect)
			.map	{ _.getWindow.isActive }
	
	windowActiveFb observe windowActive.set
	
	def start() {
		Library.init()
		
		val content	= frame.getContentPane
		content setLayout new BorderLayout
		content add (ui.component, BorderLayout.CENTER)
		frame setTitle					Style.window.title
		frame setIconImage				Style.window.icon
		frame setSize					Style.window.size
		frame setDefaultCloseOperation	WindowConstants.DO_NOTHING_ON_CLOSE
		frame onWindowClosing { _ => dispose() }
		model.start()
		frame setVisible true
		
		val macifyApplication	= new DefaultApplication
		macifyApplication addApplicationListener new ApplicationAdapter {
			override def handleQuit(ev:ApplicationEvent) {
				dispose()
			}
		}
		macifyApplication setApplicationIconImage Style.application.osxIcon
		macifyApplication.removeAboutMenuItem()
		macifyApplication.removePreferencesMenuItem()
	}
	
	private def dispose() {
		model.dispose()
		Track.dispose()
		frame.dispose()
		
		// TODO ugly, but DJane$ is referenced from Thread#contextClassLoader and there are some additional GC roots still alive in swing
		sys exit 0
	}
	
	model.speed persist (Storage.base / "speed.json")
	
	start()
}
