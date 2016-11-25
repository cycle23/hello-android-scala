package io.github.cycle23.scala_controllerclient

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import com.google.vr.sdk.base.AndroidCompat
import com.google.vr.sdk.controller.Controller
import com.google.vr.sdk.controller.Controller.ConnectionStates
import com.google.vr.sdk.controller.ControllerManager
import com.google.vr.sdk.controller.ControllerManager.ApiStatus

/**
  * Created by cody on 11/24/16.
  */
class MainActivity extends Activity {
    val TAG = "ControllerClientActivity"

    var controllerManager: ControllerManager = _
    var controller: Controller = _

    var apiStatusView: TextView = _
    var controllerStateView: TextView = _
    var controllerOrientationText: TextView = _
    var controllerTouchpadView: TextView = _
    var controllerButtonView: TextView = _

    var controllerOrientationView: OrientationView = _

    val uiHandler = new Handler()
    
    def findTextViewById(id: Int): TextView = {
        findViewById(id) match {
            case tv: TextView => tv
            case x : View => x.asInstanceOf[TextView]
            case null => throw new NullPointerException
        }
    }

    override def onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        apiStatusView = findTextViewById(R.id.api_status_view)

        controllerStateView = findTextViewById(R.id.controller_state_view)
        controllerTouchpadView = findTextViewById(R.id.controller_touchpad_view)
        controllerButtonView = findTextViewById(R.id.controller_button_view)
        controllerOrientationText = findTextViewById(R.id.controller_orientation_text)
        controllerTouchpadView = findTextViewById(R.id.controller_touchpad_view)
        controllerButtonView = findTextViewById(R.id.controller_button_view)

        val listener = EventListener()
        controllerManager = new ControllerManager(this, listener)
        apiStatusView.setText("Binding to VR Service")
        controller = controllerManager.getController
        controller.setEventListener(listener)

        controllerOrientationView = findViewById(R.id.controller_orientation_view) match {
            case ov: OrientationView => ov
            case _ => throw new ClassCastException
        }
        controllerOrientationView.setController(controller)

        AndroidCompat.setVrModeEnabled(this, true)
    }

    override def onStart(): Unit = {
        super.onStart()
        controllerManager.start()
        controllerOrientationView.startTrackingOrientation()
    }

    override def onStop(): Unit = {
        controllerManager.stop()
        controllerOrientationView.stopTrackingOrientation()
        super.onStop()
    }

    class EventListener extends Controller.EventListener
      with ControllerManager.EventListener with Runnable {

        var apiStatus = ApiStatus.toString(ApiStatus.OK)
        var controllerState = ConnectionStates.DISCONNECTED

        override def onApiStatusChanged(state: Int): Unit = {
            apiStatus = ApiStatus.toString(state)
            uiHandler.post(this)
        }

        override def onConnectionStateChanged(state: Int): Unit = {
            controllerState = state
            uiHandler.post(this)
        }

        override def onRecentered(): Unit = {
            controllerOrientationView.resetYaw()
        }

        override def onUpdate(): Unit = { uiHandler.post(this) }
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
                controllerTouchpadView.setText("[ NO TOUCH COCO ]")
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

    object EventListener {
        def apply() = new EventListener()
    }}

