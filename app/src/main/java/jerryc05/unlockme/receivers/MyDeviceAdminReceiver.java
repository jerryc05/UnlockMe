package jerryc05.unlockme.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;

@SuppressWarnings("NullableProblems")
public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

  private static int failedAttempt;

  @Override
  public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    super.onPasswordFailed(context, intent, user);

    MainActivity.threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        final MainActivity activity = MainActivity.weakMainActivity.get();
        CameraBaseAPIClass.getImageFromDefaultCamera(activity, true);
        CameraBaseAPIClass.getImageFromDefaultCamera(activity, true);
      }
    });
  }

  @Override
  public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
    super.onPasswordSucceeded(context, intent, user);

    if (failedAttempt > 0) {
      UserInterface.notifyToUI("Unsuccessful Unlock Attempt Captured",
              "UnlockMe captured " + failedAttempt +
                      " attempt(s) since last successful unlock");
    }
    failedAttempt = 0;
  }
}
