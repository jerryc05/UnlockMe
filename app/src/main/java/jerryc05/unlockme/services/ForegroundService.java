package jerryc05.unlockme.services;

import android.app.IntentService;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;

import static jerryc05.unlockme.helpers.UserInterface.getNotificationManager;
import static jerryc05.unlockme.helpers.UserInterface.notifyToForegroundService;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.EXTRA_CAMERA_FACING;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.getImageFromDefaultCamera;

public class ForegroundService extends IntentService {

  private static final String
          TAG                          = "ForegroundService";
  public static final  String
          ACTION_DISMISS_NOTIFICATION  = "ACTION_DISMISS_NOTIFICATION",
          ACTION_CAPTURE_IMAGE         = "ACTION_CAPTURE_IMAGE",
          EXTRA_CANCEL_NOTIFICATION_ID = "EXTRA_CANCEL_NOTIFICATION_ID";

  public ForegroundService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(@Nullable final Intent intent) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onHandleIntent: ");

    notifyToForegroundService(this);

    assert intent != null;
    final String action = intent.getAction();
    assert action != null;

    switch (action) {
      case ACTION_DISMISS_NOTIFICATION:
        getNotificationManager(getApplicationContext())
                .cancel(intent.getIntExtra(
                        EXTRA_CANCEL_NOTIFICATION_ID, -1));
        break;
      case ACTION_CAPTURE_IMAGE:
        getImageFromDefaultCamera(getApplicationContext(),
                intent.getBooleanExtra(
                        EXTRA_CAMERA_FACING, true));
        break;
      default:
        throw new UnsupportedOperationException("Unknown action!");
    }

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onHandleIntent: Finished! ");
  }

  @Override
  public void onTrimMemory(int level) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onTrimMemory: ");

    super.onTrimMemory(level);

    if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
      CameraBaseAPIClass.trimMemory();
    }
  }
}
