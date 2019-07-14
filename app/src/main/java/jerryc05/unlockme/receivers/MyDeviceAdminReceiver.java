package jerryc05.unlockme.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.util.Log;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.services.ForegroundIntentService;

@SuppressWarnings("NullableProblems")
public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

  private static final String TAG =
          MyDeviceAdminReceiver.class.getSimpleName();
  private static       int    failedAttempt;

  @Override
  public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    super.onPasswordFailed(context, intent);
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onPasswordFailed: ");

    final Intent mIntent = new Intent(context,
            ForegroundIntentService.class);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
      context.startService(mIntent);
    else
      context.startForegroundService(mIntent);
  }

  @Override
  public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
    super.onPasswordSucceeded(context, intent, user);

    if (failedAttempt > 0) {
      UserInterface.notifyToUI("Unsuccessful Unlock Attempt Captured",
              "UnlockMe captured " + failedAttempt +
                      " attempt(s) since last successful unlock", context);
    }
    failedAttempt = 0;
  }
}
