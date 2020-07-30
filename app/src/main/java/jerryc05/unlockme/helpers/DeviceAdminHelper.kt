package jerryc05.unlockme.helpers

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import jerryc05.unlockme.BuildConfig
import jerryc05.unlockme.MyDeviceAdminReceiver
import jerryc05.unlockme.R
import jerryc05.unlockme.activities.MainActivity
import java.util.*
import kotlin.system.exitProcess

object DeviceAdminHelper {

  private const val TAG = "DeviceAdminHelper"
  const val deviceAdminPermissionExplanation =
    "We need DEVICE ADMIN permission to work properly." //todo i18n
  private var mComponentName: ComponentName? = null

  fun requestPermission(activity: MainActivity) {
    if (BuildConfig.DEBUG) Log.d(TAG, "requestPermission: " + System.currentTimeMillis())

//    if (activity.mRequestDeviceAdminLock != null) {
//      activity.mRequestDeviceAdminLock.unlock();
//      activity.mRequestDeviceAdminLock = null;
//    }
    if (!getDevicePolicyManager(activity).isAdminActive(getComponentName(activity))) {
      val intentDeviceAdmin = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
      intentDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName)
      intentDeviceAdmin.putExtra(
        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
        deviceAdminPermissionExplanation
      )
      activity.runOnUiThread {
        activity.startActivityForResult(
          intentDeviceAdmin, MainActivity.REQUEST_CODE_DEVICE_ADMIN
        )
      }
    } else mComponentName = null
  }

  fun onRequestPermissionFinished(activity: MainActivity) {
    val applicationContext = activity.applicationContext
    if (!getDevicePolicyManager(applicationContext)
        .isAdminActive(getComponentName(applicationContext))
    ) {
//      if (activity.mRequestDeviceAdminLock == null)
//        activity.mRequestDeviceAdminLock = new ReentrantLock();
//      activity.mRequestDeviceAdminLock.lock();
      val onClickListener =
        DialogInterface.OnClickListener { dialogInterface, _ ->
          dialogInterface.dismiss()
          exitProcess(1)
        }
      AlertDialog.Builder(activity)
        .setTitle("Permission Required")
        .setMessage(deviceAdminPermissionExplanation)
        .setIcon(R.drawable.ic_round_error)
        .setCancelable(false)
        .setPositiveButton("OK", onClickListener)
        .show()
    } else mComponentName = null
  }

  private fun getDevicePolicyManager(context: Context): DevicePolicyManager {
    return Objects.requireNonNull(
      context.getSystemService(Context.DEVICE_POLICY_SERVICE)
    ) as DevicePolicyManager
  }

  private fun getComponentName(context: Context): ComponentName {
    if (mComponentName == null)
      mComponentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
    return mComponentName!!
  }
}