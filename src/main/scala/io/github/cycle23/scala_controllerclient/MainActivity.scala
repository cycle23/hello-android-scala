package io.github.cycle23.scala_controllerclient

import com.google.vr.sdk.base.AndroidCompat
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.Controller.ConnectionStates
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.controller.ControllerManager.ApiStatus
import org.scaloid.common._

/**
  * Created by cody on 11/24/16.
  */

class MainActivity extends SActivity with TypedFindView {
  lazy val apiStatusView = findView(TR.api_status_view)
  lazy val controllerStateView = findView(TR.controller_state_view)
  lazy val controllerOrientationText = findView(TR.controller_orientation_text)
  lazy val controllerTouchpadView = findView(TR.controller_touchpad_view)
  lazy val controllerButtonView = findView(TR.controller_button_view)

  lazy val controllerOrientationView = findView(TR.controller_orientation_view)

  private val listener = EventListener()
  val controllerManager = new ControllerManager(this, listener)
  val controller = controllerManager.getController
  controller.setEventListener(listener)

  onCreate {
    setContentView(R.layout.main_layout)

    apiStatusView.setText("Binding to VR Service")
    controllerOrientationView.setController(controller)

    AndroidCompat.setVrModeEnabled(this, true)
  }

  onStart {
    controllerManager.start()
    controllerOrientationView.startTrackingOrientation()
  }

  onStop {
    controllerOrientationView.stopTrackingOrientation()
    controllerManager.stop()
  }

  private class EventListener extends Controller.EventListener
    with ControllerManager.EventListener with Runnable {

    var apiStatus = ApiStatus.toString(ApiStatus.OK)
    var controllerState = ConnectionStates.DISCONNECTED

    override def onApiStatusChanged(state: Int): Unit = {
      apiStatus = ApiStatus.toString(state)
      handler.post(this)
    }

    override def onConnectionStateChanged(state: Int): Unit = {
      controllerState = state
      handler.post(this)
    }

    override def onRecentered(): Unit = {
      controllerOrientationView.resetYaw()
    }

    override def onUpdate(): Unit = { handler.post(this) }

    override def run(): Unit = {
      apiStatusView.setText(apiStatus)
      controllerStateView.setText(ConnectionStates.toString(controllerState))
      controller.update()
      controllerOrientationText.setText(
        " " + controller.orientation + "\n" + controller.orientation.toAxisAngleString
      )
      if (controller.isTouching) {
        controllerTouchpadView.setText(s"[COCO - ${controller.touch.x%4.2f}, ${controller.touch.y%4.2f}]")
      }
      else {
        controllerTouchpadView.setText("[ NO TOUCH COCO2 ]")
      }
      controllerButtonView.setText(
        s"[${if (controller.appButtonState) "A" else " "}]" +
          s"[${if (controller.homeButtonState) "H" else " "}]" +
          s"[${if (controller.clickButtonState) "T" else " "}]" +
          s"[${if (controller.volumeUpButtonState) "+" else " "}]" +
          s"[${if (controller.volumeDownButtonState) "-" else " "}]"
      )
    }
  }

  private object EventListener {
    def apply() = new EventListener()
  }
}

