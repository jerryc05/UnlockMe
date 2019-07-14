package jerryc05.unlockme.services;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;

import static jerryc05.unlockme.helpers.UserInterface.getNotificationManager;

public class ForegroundIntentService extends IntentService {

  private static final String TAG =
          ForegroundIntentService.class.getSimpleName();

  public ForegroundIntentService() {
    super("ForegroundIntentService");
  }

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onCreate: ");

    final String title = "UnlockMe Background Service";
    final Notification.Builder builder =
            new Notification.Builder(this)
            .setContentTitle(title)
//            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground);
    startForeground(-1, UserInterface.setNotificationChannel(builder,
            getNotificationManager(), title,
            "Background service notification for UnlockMe.",
            true).build());
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onHandleIntent: ");

    CameraBaseAPIClass.getImageFromDefaultCamera(
            this, true);
  }
}
