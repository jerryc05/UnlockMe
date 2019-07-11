package jerryc05.unlockme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.helpers.Camera2APIHelper;
import jerryc05.unlockme.helpers.DeviceAdminHelper;
import jerryc05.unlockme.helpers.URLConnectionBuilder;

@SuppressWarnings("NullableProblems")
public final class MainActivity extends Activity
        implements View.OnClickListener {

  public final static int
          REQUEST_CODE_DEVICE_ADMIN              = 0,
          REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL = 1;

  public        ReentrantLock      requestDeviceAdminLock;
  public static Context            applicationContext;
  public static WeakReference<MainActivity> weakMainActivity;
  public static ThreadPoolExecutor threadPoolExecutor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getThreadPoolExecutor().execute(new Runnable() {
      @Override
      public void run() {
        applicationContext = getApplicationContext();
        weakMainActivity=new WeakReference<>(MainActivity.this);
        findViewById(R.id.activity_main_button_front)
                .setOnClickListener(MainActivity.this);
        findViewById(R.id.activity_main_button_back)
                .setOnClickListener(MainActivity.this);
        checkUpdate();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();

    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        if (requestDeviceAdminLock != null)
          requestDeviceAdminLock.lock();
        DeviceAdminHelper.requestPermission(MainActivity.this);
      }
    });
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();

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
      Camera2APIHelper.onRequestPermissionFinished(
              this, grantResults);
  }

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.activity_main_button_front ||
            view.getId() == R.id.activity_main_button_back)
      threadPoolExecutor.execute(new Runnable() {
        @Override
        public void run() {
          Camera2APIHelper.automaticTakePhoto(MainActivity.this,
                  view.getId() == R.id.activity_main_button_back
                          ? CameraCharacteristics.LENS_FACING_BACK
                          : CameraCharacteristics.LENS_FACING_FRONT);
        }
      });
  }

  private static ThreadPoolExecutor getThreadPoolExecutor() {
    if (threadPoolExecutor == null)
      threadPoolExecutor = new ThreadPoolExecutor(1, 5,
              5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
    return threadPoolExecutor;
  }

  protected void checkUpdate() {
    final String
            keyword = "/jerryc05/UnlockMe/tree",
            URL = "https://www.github.com/jerryc05/UnlockMe/releases";

    try (final URLConnectionBuilder connectionBuilder = URLConnectionBuilder
            .get(URL)
            .setConnectTimeout(1000)
            .setReadTimeout(1000)
            .connect()) {
      String result = connectionBuilder.getResult();
      result = result.substring(result.indexOf(keyword) +
              keyword.length() + 2);
      final String latest = result.substring(0, result.indexOf('"'));

      if (!latest.equals(BuildConfig.VERSION_NAME))
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("New Version Available")
                    .setMessage("Do you want to upgrade from\n" +
                            BuildConfig.VERSION_NAME + "  to  " + latest + '?')
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialogInterface,
                                                  int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(URL + "/tag/v" + latest)));
                              }
                            })
                    .setNegativeButton("NO", null)
                    .show();
          }
        });

    } catch (final Exception e) {
      alertExceptionToUI(e);
    }
  }

  public void alertExceptionToUI(Exception e) {
    alertExceptionToUI(e, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        throw new UnsupportedOperationException(e);
      }
    });
  }

  public void alertExceptionToUI(Exception e,
                                 DialogInterface.OnClickListener onClickListener) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Crash Report")
                .setMessage(e.toString())
                .setIcon(R.drawable.ic_round_error_24px)
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }
}
