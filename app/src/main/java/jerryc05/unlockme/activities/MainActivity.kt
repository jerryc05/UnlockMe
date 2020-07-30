package jerryc05.unlockme.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import jerryc05.unlockme.BuildConfig
import jerryc05.unlockme.MyBaseApp.Companion.threadPoolExecutor
import jerryc05.unlockme.R
import jerryc05.unlockme.databinding.ActivityMainBinding
import jerryc05.unlockme.helpers.DeviceAdminHelper
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass
import java.io.ByteArrayOutputStream

class MainActivity :
  AppCompatActivity(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

  companion object {
    private const val TAG = "MainActivity"
    const val REQUEST_CODE_DEVICE_ADMIN = 0
    const val REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL = 1
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
  }

  //  var mRequestDeviceAdminLock: ReentrantLock? = null
  private lateinit var mBinding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    if (BuildConfig.DEBUG) Log.d(TAG, "onCreate: ")
    super.onCreate(savedInstanceState)
    mBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(mBinding.root)

    // init view
    threadPoolExecutor.execute {
      mBinding.activityMainButtonFront.setOnClickListener(this)
      mBinding.activityMainButtonBack.setOnClickListener(this)
      mBinding.activityMainApi1Switch.apply {
        isChecked = !CameraBaseAPIClass.getPreferCamera2(applicationContext)
        setOnCheckedChangeListener(this@MainActivity)
      }
    }

    // init camera
    threadPoolExecutor.execute {
      cameraProvider = ProcessCameraProvider.getInstance(this).get()
      imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation?.let { setTargetRotation(it) }
          }
        }
        .build()
    }
  }

  override fun onResume() {
    if (BuildConfig.DEBUG) Log.d(TAG, "onResume: ")
    super.onResume()

    threadPoolExecutor.execute {
//      if (mRequestDeviceAdminLock != null) mRequestDeviceAdminLock!!.lock()
      DeviceAdminHelper.requestPermission(this) //todo
      CameraBaseAPIClass.requestPermissions(this)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RESULT_CANCELED &&
      resultCode == REQUEST_CODE_DEVICE_ADMIN
    ) DeviceAdminHelper.onRequestPermissionFinished(this) //todo
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL) CameraBaseAPIClass.onRequestPermissionFinished(
      applicationContext, grantResults
    )
  }

  override fun onClick(view: View) {
    if (BuildConfig.DEBUG) Log.d(TAG, "onClick: ")

    threadPoolExecutor.execute {
      when (view.id) {
        R.id.activity_main_button_front -> takePicture(CameraSelector.DEFAULT_FRONT_CAMERA)
        R.id.activity_main_button_back -> takePicture(CameraSelector.DEFAULT_BACK_CAMERA)
      }
    }
  }

  private fun takePicture(cameraSelector: CameraSelector) {
    try {
      runOnUiThread {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
          this, cameraSelector, imageCapture
        )
      }

      val byteArrOS = ByteArrayOutputStream()
      val outputOptions = ImageCapture.OutputFileOptions.Builder(byteArrOS).build()

      class OnImgSavedCB : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val msg = "Photo capture succeeded (${byteArrOS.size()} bytes) @ ${output.savedUri}"
          runOnUiThread {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
          }
          Log.d(TAG, msg)
        }

        override fun onError(exc: ImageCaptureException) {
          Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
        }
      }

      imageCapture.takePicture(outputOptions, threadPoolExecutor, OnImgSavedCB())

    } catch (exc: Exception) {
      Log.d(TAG, "Use case binding failed", exc)
    }
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    if (buttonView.id == R.id.activity_main_api1Switch)
      getSharedPreferences(CameraBaseAPIClass.SP_NAME_CAMERA, MODE_PRIVATE)
        .edit()
        .putBoolean(CameraBaseAPIClass.SP_KEY_PREFER_CAMERA_API_2, !isChecked)
        .apply()
  }

  /*@WorkerThread
  private void checkUpdate() {
    URLConnectionBuilder connectionBuilder = null;
    try {
      final String
              URL = "https://api.github.com/repos/jerryc05/UnlockMe/tags";
      connectionBuilder = URLConnectionBuilder
              .get(URL)
              .setConnectTimeout(1000)
              .setReadTimeout(1000)
              .setUseCache(false)
              .connect(getApplicationContext());

      final String latest = new JSONArray(connectionBuilder.getResult())
              .getJSONObject(0)
              .getString("name").substring(1);

      if (!latest.equals(BuildConfig.VERSION_NAME)) {
        if (BuildConfig.DEBUG)
          Log.d(TAG, "checkUpdate: " + latest);

        final String tagURL = "https://github.com/jerryc05/UnlockMe/releases/tag/v";
        final DialogInterface.OnClickListener positive = (dialogInterface, i) -> {
          dialogInterface.dismiss();
          startActivity(new Intent(Intent.ACTION_VIEW,
                  Uri.parse(tagURL + latest)));
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("New Version Available")
                .setMessage("Do you want to upgrade from\n\tv" +
                        BuildConfig.VERSION_NAME + "  to  v" + latest + '?')
                .setPositiveButton("YES", positive)
                .setNegativeButton("NO", null)
                .setIcon(R.drawable.ic_round_info);

        runOnUiThread(builder::show);
      }

    } catch (final UnknownHostException e) {
      UserInterface.showExceptionToNotification(getApplicationContext(),
              "Cannot connect to github.com!\n>>> " + e.toString(),
              "checkUpdate()");

    } catch (final Exception e) {
      if (!WIFI_ONLY_EXCEPTION_PROMPT.equals(e.getMessage()))
        UserInterface.showExceptionToNotification(getApplicationContext(),
                e.toString(), "checkUpdate()");

    } finally {
      if (connectionBuilder != null)
        connectionBuilder.disconnect();
    }
  }*/

  override fun onDestroy() {
    if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy: ")
    super.onDestroy()
  }
}