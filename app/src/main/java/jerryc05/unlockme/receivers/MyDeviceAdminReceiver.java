package jerryc05.unlockme.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.os.UserHandle;
import android.util.Log;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;

@SuppressWarnings("NullableProblems")
public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

  private final static String TAG = MyDeviceAdminReceiver.class.getSimpleName();
  private static       int    failedAttempt;

  @Override
  public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    super.onPasswordFailed(context, intent, user);

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onPasswordFailed: " + ++failedAttempt);
    MainActivity.threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        CameraBaseAPIClass.getImageFromDefaultCamera(
                MainActivity.weakMainActivity.get(),true);
      }
    });
  }

  @Override
  public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
    super.onPasswordSucceeded(context, intent, user);

    if (failedAttempt > 0) {

    }
    failedAttempt = 0;
  }
}
