package jerryc05.unlockme.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import org.json.JSONArray;

import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.DeviceAdminHelper;
import jerryc05.unlockme.helpers.URLConnectionBuilder;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;
import jerryc05.unlockme.services.ForegroundService;

import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.SP_KEY_PREFER_CAMERA_API_2;

@SuppressWarnings("NullableProblems")
public final class MainActivity extends Activity
        implements OnClickListener, OnCheckedChangeListener {

  private final static String
          TAG                                    = MainActivity.class.getSimpleName();
  public final static  int
          REQUEST_CODE_DEVICE_ADMIN              = 0,
          REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL = 1;

  public        ReentrantLock      requestDeviceAdminLock;
  public static ThreadPoolExecutor threadPoolExecutor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.activity_main_button_front)
            .setOnClickListener(MainActivity.this);
    findViewById(R.id.activity_main_button_back)
            .setOnClickListener(MainActivity.this);
    findViewById(R.id.activity_main_button_stopService)
            .setOnClickListener(MainActivity.this);

    final Switch forceAPI1 =
            findViewById(R.id.activity_main_api1Switch);
    forceAPI1.setOnCheckedChangeListener(MainActivity.this);
    forceAPI1.setChecked(!CameraBaseAPIClass.getPreferCamera2(
            MainActivity.this));
  }

  @Override
  protected void onStart() {
    super.onStart();

    getThreadPoolExecutor().execute(new Runnable() {
      @Override
      public void run() {
        if (requestDeviceAdminLock != null)
          requestDeviceAdminLock.lock();
        DeviceAdminHelper.requestPermission(MainActivity.this);
        checkUpdate();
      }
    });
  }

  @Override
  protected void onStop() {
    super.onStop();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onStop: ");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onDestroy: ");
    if (threadPoolExecutor != null) {
      threadPoolExecutor.shutdownNow();
      threadPoolExecutor = null;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_DEVICE_ADMIN)
      DeviceAdminHelper.onRequestPermissionFinished(this);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    if (requestCode == REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL)
      CameraBaseAPIClass.onRequestPermissionFinished(this, grantResults);
  }

  @Override
  public void onClick(View view) {
    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        final int id = view.getId();
        final Intent mIntent = new Intent(MainActivity.this,
                ForegroundService.class);

        if (id == R.id.activity_main_button_stopService)
          stopService(mIntent);

        else if (CameraBaseAPIClass.requestPermissions(
                MainActivity.this)) {
          CameraBaseAPIClass.isFront = id == R.id.activity_main_button_front;
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            startService(mIntent);
          else
            startForegroundService(mIntent);
        }
      }
    });
  }

  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
    if (compoundButton.getId() == R.id.activity_main_api1Switch)
      getSharedPreferences(CameraBaseAPIClass.SP_NAME_CAMERA,
              Context.MODE_PRIVATE).edit()
              .putBoolean(SP_KEY_PREFER_CAMERA_API_2, !b)
              .apply();
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
                          + "\non MainActivity rejected:\n >>> "
                          + runnable.toString(),
                  "threadPoolExecutor#rejectedExecution()",
                  MainActivity.this);
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

  void checkUpdate() {
    URLConnectionBuilder connectionBuilder = null;
    try {
      final String
              URL = "https://api.github.com/repos/jerryc05/UnlockMe/tags";
      connectionBuilder = URLConnectionBuilder
              .get(URL)
              .setConnectTimeout(1000)
              .setReadTimeout(1000)
              .setUseCache(false)
              .connect(this);

      final String latest = new JSONArray(
              connectionBuilder.getResult()).getJSONObject(0)
              .getString("name").substring(1);

      if (!latest.equals(BuildConfig.VERSION_NAME))
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final String URL = "https://github.com/jerryc05/UnlockMe/releases/tag/v";
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("New Version Available")
                    .setMessage("Do you want to upgrade from\n\tv" +
                            BuildConfig.VERSION_NAME + "  to  v" + latest + '?')
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialogInterface,
                                                  int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(URL + latest)));
                              }
                            })
                    .setNegativeButton("NO", null)
                    .setIcon(R.drawable.ic_round_info_24px)
                    .show();
          }
        });

    } catch (final UnknownHostException e) {
      UserInterface.showExceptionToNotificationNoRethrow(
              "Cannot connect to github.com!\n>>> " + e.toString(),
              "checkUpdate()", this);
    } catch (final IllegalStateException e) {
      UserInterface.notifyToUI("Update Interrupted",
              "Will not connect through cellular data!", this);
    } catch (final Exception e) {
      UserInterface.showExceptionToNotificationNoRethrow(
              e.toString(), "checkUpdate()", this);
    } finally {
      if (connectionBuilder != null)
        connectionBuilder.disconnect();
    }
  }
}
