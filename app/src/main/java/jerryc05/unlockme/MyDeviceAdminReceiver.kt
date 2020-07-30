package jerryc05.unlockme

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import jerryc05.unlockme.helpers.DeviceAdminHelper
import jerryc05.unlockme.helpers.UserInterface

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

  override fun onPasswordFailed(context: Context, _intent: Intent) {
    if (BuildConfig.DEBUG) Log.d(TAG, "onPasswordFailed: ")

    val intent = Intent(context, MyIntentService::class.java)
    intent.action = MyIntentService.ACTION_CAPTURE_IMAGE

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
      context.startService(intent)
    else
      context.startForegroundService(intent)
  }

  override fun onPasswordSucceeded(context: Context, intent: Intent) {
    if (failedAttempt > 0) UserInterface.notifyToUI(
      "Unsuccessful Unlock Attempt Captured",
      "UnlockMe captured " + failedAttempt +
              " attempt(s) since last successful unlock", context
    ) //todo didn't show
    failedAttempt = 0
  }

//  override fun onEnabled(context: Context, intent: Intent) {
//    // Maybe notification here
//  }

  override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
    return (DeviceAdminHelper.deviceAdminPermissionExplanation
            + " Or it will not work properly!")
  }

//  override fun onDisabled(context: Context, intent: Intent) {
//    // Maybe notification here
//  }

  companion object {
    private const val TAG = "MyDeviceAdminReceiver"
    private var failedAttempt = 0
  }
}