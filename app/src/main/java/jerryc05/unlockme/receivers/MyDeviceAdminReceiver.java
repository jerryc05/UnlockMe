package jerryc05.unlockme.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.services.ForegroundService;

import static jerryc05.unlockme.services.ForegroundService.ACTION_CAPTURE_IMAGE;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

  private static final String TAG = "MyDeviceAdminReceiver";
  private static       int    failedAttempt;

  @Override
  public void onPasswordFailed(@NonNull final Context context,
                               @NonNull Intent intent) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onPasswordFailed: ");

    intent = new Intent(context, ForegroundService.class);
    intent.setAction(ACTION_CAPTURE_IMAGE);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
      context.startService(intent);
    else
      context.startForegroundService(intent);
  }

  @Override
  public void onPasswordSucceeded(@NonNull final Context context,
                                  @NonNull final Intent intent) {
    if (failedAttempt > 0)
      UserInterface.notifyToUI("Unsuccessful Unlock Attempt Captured",
              "UnlockMe captured " + failedAttempt +
                      " attempt(s) since last successful unlock", context); //todo didn't show
    failedAttempt = 0;
  }
}
