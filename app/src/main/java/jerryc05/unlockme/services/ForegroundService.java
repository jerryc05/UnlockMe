package jerryc05.unlockme.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

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
          TAG                        = ForegroundService.class.getSimpleName();
  public static final String
          ACTION_UPDATE_NOTIFICATION = "ACTION_UPDATE_NOTIFICATION",
          ACTION_CAPTURE_IMAGE       = "ACTION_CAPTURE_IMAGE";
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
  public int onStartCommand(Intent intent, int flags, int startId) {
    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_UPDATE_NOTIFICATION:
              notifyToForegroundService(ForegroundService.this);
              break;
            case ACTION_CAPTURE_IMAGE:
              getImageFromDefaultCamera(ForegroundService.this,
                      intent.getBooleanExtra(
                              EXTRA_CAMERA_FACING, true));
              break;
            default:
              throw new UnsupportedOperationException("Unknown action!");
          }
        } catch (Exception e) {
          UserInterface.showExceptionToNotification(e.toString(),
                  "onStartCommand()", ForegroundService.this);
        }
      }
    });

    return super.onStartCommand(intent, flags, startId);
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

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
