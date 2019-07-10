package jerryc05.unlockme.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

import jerryc05.unlockme.BuildConfig;

@SuppressWarnings("NullableProblems")
public class MyDAReceiver extends DeviceAdminReceiver {

  private final static String TAG = MyDAReceiver.class.getSimpleName();
  private static       int    failedAttempt;

  @Override
  public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    super.onPasswordFailed(context, intent, user);

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onPasswordFailed: " + ++failedAttempt);
  }

  @Override
  public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
    super.onPasswordSucceeded(context, intent, user);

    if (failedAttempt > 0) {

    }
    failedAttempt = 0;
  }
}
