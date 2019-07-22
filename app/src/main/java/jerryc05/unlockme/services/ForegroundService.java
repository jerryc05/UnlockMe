package jerryc05.unlockme.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.receivers.MyDeviceAdminReceiver;

import static jerryc05.unlockme.helpers.UserInterface.notifyToForegroundService;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.EXTRA_CAMERA_FACING;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.getImageFromDefaultCamera;

public class ForegroundService extends Service {

  static final        String
          TAG                          = ForegroundService.class.getSimpleName();
  public static final String
          ACTION_DISMISS_NOTIFICATION  = "ACTION_DISMISS_NOTIFICATION",
          ACTION_CAPTURE_IMAGE         = "ACTION_CAPTURE_IMAGE",
          EXTRA_CANCEL_NOTIFICATION_ID = "EXTRA_CANCEL_NOTIFICATION_ID";
  private ThreadPoolExecutor threadPoolExecutor;
  MyDeviceAdminReceiver myDeviceAdminReceiver;

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onCreate: ");

    getThreadPoolExecutor().execute(new Runnable() {
      @Override
      public void run() {
        notifyToForegroundService(ForegroundService.this);

        final IntentFilter intentFilter = new IntentFilter(
                "android.app.action.DEVICE_ADMIN_ENABLED");
        intentFilter.addAction("android.app.action.ACTION_PASSWORD_FAILED");
        intentFilter.addAction("android.app.action.ACTION_PASSWORD_SUCCEEDED");

        if (myDeviceAdminReceiver == null)
          myDeviceAdminReceiver = new MyDeviceAdminReceiver();
        registerReceiver(myDeviceAdminReceiver, intentFilter);
      }
    });
  }

  @Override
  public int onStartCommand(@NonNull final Intent intent,
                            final int flags, final int startId) {
    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_DISMISS_NOTIFICATION:
              UserInterface.getNotificationManager(ForegroundService.this)
                      .cancel(intent.getIntExtra(
                              EXTRA_CANCEL_NOTIFICATION_ID, -1));
              break;
            case ACTION_CAPTURE_IMAGE:
              getImageFromDefaultCamera(ForegroundService.this,
                      intent.getBooleanExtra(
                              EXTRA_CAMERA_FACING, true));
              break;
            default:
              throw new UnsupportedOperationException("Unknown action!");
          }
        } catch (final Exception e) {
          UserInterface.showExceptionToNotification(e.toString(),
                  "onStartCommand()", ForegroundService.this);
        }
      }
    });

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onDestroy: ");

    unregisterReceiver(myDeviceAdminReceiver);
    if (threadPoolExecutor != null) {
      threadPoolExecutor.shutdown();
      threadPoolExecutor = null;
    }
  }

  private ThreadPoolExecutor getThreadPoolExecutor() {
    if (threadPoolExecutor == null) {
      RejectedExecutionHandler rejectedExecutionHandler
              = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable runnable,
                                      ThreadPoolExecutor threadPoolExecutor) {
          UserInterface.showExceptionToNotificationNoRethrow(
                  "ThreadPoolExecutor：\n>>> "
                          + threadPoolExecutor.toString()
                          + "\non " + TAG + " rejected:\n >>> "
                          + runnable.toString(),
                  "threadPoolExecutor#rejectedExecution()",
                  ForegroundService.this);
        }
      };
      final int processorCount = Runtime.getRuntime().availableProcessors();
      threadPoolExecutor = new ThreadPoolExecutor(processorCount,
              2 * processorCount, 5, TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(1), rejectedExecutionHandler);
      threadPoolExecutor.allowCoreThreadTimeOut(true);
    }
    return threadPoolExecutor;
  }

  @Override
  public IBinder onBind(@NonNull final Intent intent) {
    return null;
  }
}
