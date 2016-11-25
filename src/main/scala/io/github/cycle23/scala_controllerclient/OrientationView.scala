package io.github.cycle23.scala_controllerclient

/**
  * Created by cody on 11/25/16.
  */

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLException
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.text.TextUtils
import android.util.AttributeSet
import com.google.vr.sdk.controller.Controller
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
  * View that graphically demonstrates the orientation of the Daydream Controller. It renders an RGB
  * box with the same orientation as the physical controller. Rotating the phone will rotate the GL
  * camera.
  */
class OrientationView(context: Context, attributeSet: AttributeSet) extends GLSurfaceView(context, attributeSet) {

  // Tracks the orientation of the phone. This would be a head pose if this was a VR application.
  // See the {@link Sensor.TYPE_ROTATION_VECTOR} section of {@link SensorEvent.values} for details
  // about the specific sensor that is used. It's important to note that this sensor defines the
  // Z-axis as point up and the Y-axis as pointing toward what the phone believes to be magnetic
  // north. Google VR's coordinate system defines the Y-axis as pointing up and the Z-axis as
  // pointing toward the user. This requires a 90-degree rotation on the X-axis to convert between
  // the two coordinate systems.
  val phoneInWorldSpaceMatrix = Array.ofDim[Float](16)

  // Tracks the orientation of the physical controller in its properly centered Start Space.
  var controller: Controller = _
  /** See {@link #resetYaw} */
  var startFromSensorTransformation: Array[Float] = _
  val controllerInStartSpaceMatrix =  Array.ofDim[Float](16)

  setEGLContextClientVersion(2)
  setEGLConfigChooser(8, 8, 8, 8, 16, 0)
  setRenderer(new Renderer())

  val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) match {
    case sm: SensorManager => sm
    case _ => throw new ClassCastException
  }
  val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  val phoneOrientationListener = new PhoneOrientationListener()

  /**
    * Bind the controller used for rendering.
    */
  def setController(controller: Controller) : Unit = {
    this.controller = controller
  }

  /**
    * Start the orientation sensor only when the app is visible.
    */
  def startTrackingOrientation() : Unit = {
    sensorManager.registerListener(
      phoneOrientationListener, orientationSensor, SensorManager.SENSOR_DELAY_GAME)
  }

  /**
    * This is similar to {@link com.google.vr.sdk.base.GvrView#recenterHeadTracker}.
    */
  def resetYaw() : Unit = {
    startFromSensorTransformation = null
  }

  /**
    * Stop the orientation sensor when the app is dismissed.
    */
  def stopTrackingOrientation() : Unit = {
    sensorManager.unregisterListener(phoneOrientationListener);
  }

  class PhoneOrientationListener extends SensorEventListener {

    override def onSensorChanged(event: SensorEvent) : Unit = {
      SensorManager.getRotationMatrixFromVector(phoneInWorldSpaceMatrix, event.values)
      if (startFromSensorTransformation == null) {
        // Android's hardware uses radians, but OpenGL uses degrees. Android uses
        // [yaw, pitch, roll] for the order of elements in the orientation array.
        val orientationRadians =
          SensorManager.getOrientation(phoneInWorldSpaceMatrix, Array.ofDim[Float](3))
        startFromSensorTransformation = Array.ofDim[Float](3)
        for (i <- Range(0,3)) {
          startFromSensorTransformation(i) = Math.toDegrees(orientationRadians(i).toDouble).toFloat
        }
      }
    }

    override def onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
  }

  class Renderer extends GLSurfaceView.Renderer {

    // Size of the 3D space where the box is rendered.
    val VIEW_SIZE = 6f

    // Basic shaders to render colored lines.
    val vertexShaderCode = TextUtils.join("\n",  Array[AnyRef](
      "uniform mat4 uMvpMatrix;",
      "attribute vec4 aPosition;",
      "attribute vec4 aColor;",
      "varying vec4 vColor;",

      "void main() {",
      "  gl_Position = uMvpMatrix * aPosition;",
      "  vColor = aColor;",
      "}"
    ))

    val fragmentShaderCode = TextUtils.join("\n",  Array[AnyRef](
      "precision mediump float;",
      "varying vec4 vColor;",

      "void main() {",
      "  gl_FragColor = vColor;",
      "}"
    ))

    // Camera projection matrix.
    val projectionMatrix = Array.ofDim[Float](16)
    // Final model-view-projection matrix passed to the shader.
    val mvpMatrix = Array.ofDim[Float](16)

    // The box is a cube with XYZ lines colored RGB, respectively. Lines get brighter in the
    // positive direction. It has a green arrow to represent the trackpad at the front and top of
    // the controller
    val vertexCount = 2 * (12 + 4) // 2 points * (12 cube edges * 4 diamond edges)
    // Set up geometry
    // 16 lines * 2 points * XYZ
    val boxVertexData = Array[Float](
      // X-aligned lines of length 4
      -2, -1, -3,  2, -1, -3,
      -2, -1,  3,  2, -1,  3,
      -2,  1, -3,  2,  1, -3,
      -2,  1,  3,  2,  1,  3,
      // Y-aligned lines of length 2
      -2, -1, -3, -2,  1, -3,
      -2, -1,  3, -2,  1,  3,
      2, -1, -3,  2,  1, -3,
      2, -1,  3,  2,  1,  3,
      // Z-aligned lines of length 6
      -2, -1, -3, -2, -1,  3,
      -2,  1, -3, -2,  1,  3,
      2, -1, -3,  2, -1,  3,
      2,  1, -3,  2,  1,  3,
      // Trackpad diamond
      -1,  1, -1,  0,  1,  0,
      0,  1,  0,  1,  1, -1,
      1,  1, -1,  0,  1, -3,
      0,  1, -3, -1,  1, -1
    )

    var buffer = ByteBuffer.allocateDirect(boxVertexData.length * 4)
    buffer.order(ByteOrder.nativeOrder())
    val boxVertices = buffer.asFloatBuffer()
    boxVertices.put(boxVertexData)
    boxVertices.position(0)

    // The XYZ lines are RGB in the positive direction and black in the negative direction.
    // 16 lines * 2 points * RGBA
    val boxColorData = Array[Float](
      // X-aligned lines
      0, 0, 0, 1, 1, 0, 0, 1,
      0, 0, 0, 1, 1, 0, 0, 1,
      0, 0, 0, 1, 1, 0, 0, 1,
      0, 0, 0, 1, 1, 0, 0, 1,
      // Y-aligned lines
      0, 0, 0, 1, 0, 1, 0, 1,
      0, 0, 0, 1, 0, 1, 0, 1,
      0, 0, 0, 1, 0, 1, 0, 1,
      0, 0, 0, 1, 0, 1, 0, 1,
      // Z-aligned lines
      0, 0, 0, 1, 0, 0, 1, 1,
      0, 0, 0, 1, 0, 0, 1, 1,
      0, 0, 0, 1, 0, 0, 1, 1,
      0, 0, 0, 1, 0, 0, 1, 1,
      // Trackpad
      0, 1, 0, 1, 0, 1, 0, 1,
      0, 1, 0, 1, 0, 1, 0, 1,
      0, 1, 0, 1, 0, 1, 0, 1,
      0, 1, 0, 1, 0, 1, 0, 1
    )
    buffer = ByteBuffer.allocateDirect(boxColorData.length * 4)
    buffer.order(ByteOrder.nativeOrder())
    val boxColors = buffer.asFloatBuffer()
    boxColors.put(boxColorData)
    boxColors.position(0)

    var program: Int = -1
    // Initialize shaders and geometry.
    override def onSurfaceCreated(unused: GL10, config: EGLConfig): Unit = {
      // Set up shaders
      val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
      GLES20.glShaderSource(vertexShader, vertexShaderCode)
      GLES20.glCompileShader(vertexShader)

      val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
      GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
      GLES20.glCompileShader(fragmentShader)

      program = GLES20.glCreateProgram()
      GLES20.glAttachShader(program, vertexShader)
      GLES20.glAttachShader(program, fragmentShader)
      GLES20.glLinkProgram(program)

      val linkStatus = Array.ofDim[Int](1)
      GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus(0) != GLES20.GL_TRUE) {
        throw new GLException(linkStatus(0),
        "Unable to create shader program: " + GLES20.glGetProgramInfoLog(program));
      }

    }

    // Set up GL environment.
    override def onSurfaceChanged(gl: GL10, width: Int, height: Int): Unit = {
      // Camera.
      GLES20.glViewport(0, 0, width, height)
      Matrix.perspectiveM(projectionMatrix, 0, 90, width.toFloat / height, 1, 2 * VIEW_SIZE)

      // Styles.
      GLES20.glClearColor(1, 1, 1, 1)
      GLES20.glLineWidth(10)

      GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    // The Matrix class requires preallocated arrays for calculations. To avoid allocating new
    // variables per-frame, these are temp arrays used during calculation in onDrawFrame.
    val tmpMatrix1 = Array.ofDim[Float](16)
    val tmpMatrix2 = Array.ofDim[Float](16)

    override def onDrawFrame(unused: GL10): Unit = {
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT)
      GLES20.glUseProgram(program)

      // Set up camera.
      Matrix.setIdentityM(tmpMatrix1, 0)

      // Convert world space to head space.
      Matrix.translateM(tmpMatrix1, 0, 0, 0, -VIEW_SIZE)
      Matrix.multiplyMM(tmpMatrix2, 0, tmpMatrix1, 0, phoneInWorldSpaceMatrix, 0)

      // Phone's Z faces up. We need it to face toward the user.
      Matrix.rotateM(tmpMatrix2, 0, 90, 1, 0, 0)

      if (startFromSensorTransformation != null) {
        // Compensate for the yaw by rotating in the other direction.
        Matrix.rotateM(tmpMatrix2, 0, -startFromSensorTransformation(0), 0, 1, 0)
      } // Else we're in a transient state between a resetYaw call and an onSensorChanged call.

      // Convert object space to world space.
      if (controller != null) {
        controller.update()
        controller.orientation.toRotationMatrix(controllerInStartSpaceMatrix)
      }
      Matrix.multiplyMM(tmpMatrix1, 0, tmpMatrix2, 0, controllerInStartSpaceMatrix, 0)

      // Set mvpMatrix.
      val mvp = GLES20.glGetUniformLocation(program, "uMvpMatrix")
      Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tmpMatrix1, 0)
      GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0)

      // Draw.
      val position = GLES20.glGetAttribLocation(program, "aPosition")
      GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 0, boxVertices)
      GLES20.glEnableVertexAttribArray(position)

      val color = GLES20.glGetAttribLocation(program, "aColor")
      GLES20.glVertexAttribPointer(color, 4, GLES20.GL_FLOAT, false, 0, boxColors)
      GLES20.glEnableVertexAttribArray(color)

      GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount)
      GLES20.glDisableVertexAttribArray(position)
      GLES20.glDisableVertexAttribArray(color)
    }
  }
}
