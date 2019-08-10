package jerryc05.unlockme.activities;

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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.DeviceAdminHelper;
import jerryc05.unlockme.helpers.URLConnectionBuilder;
import jerryc05.unlockme.helpers.UserInterface;
import jerryc05.unlockme.helpers.camera.CameraBaseAPIClass;
import jerryc05.unlockme.services.ForegroundService;

import static jerryc05.unlockme.helpers.URLConnectionBuilder.WIFI_ONLY_EXCEPTION_PROMPT;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.EXTRA_CAMERA_FACING;
import static jerryc05.unlockme.helpers.camera.CameraBaseAPIClass.SP_KEY_PREFER_CAMERA_API_2;
import static jerryc05.unlockme.services.ForegroundService.ACTION_CAPTURE_IMAGE;

public final class MainActivity extends _MyActivity
        implements OnClickListener, OnCheckedChangeListener {

  private final static String
          TAG                                    = "MainActivity";
  public final static  int
          REQUEST_CODE_DEVICE_ADMIN              = 0,
          REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL = 1;

  public ReentrantLock requestDeviceAdminLock; // todo

  @IntDef({REQUEST_CODE_DEVICE_ADMIN, REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL})
  @Retention(RetentionPolicy.SOURCE)
  private @interface RequestCodes {
  }

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getThreadPoolExecutor(getApplicationContext()).execute(() -> {
              findViewById(R.id.activity_main_button_front)
                      .setOnClickListener(MainActivity.this);
              findViewById(R.id.activity_main_button_back)
                      .setOnClickListener(MainActivity.this);
              findViewById(R.id.activity_main_button_stopService)
                      .setOnClickListener(MainActivity.this);

              final Switch forceAPI1 = findViewById(R.id.activity_main_api1Switch);
              forceAPI1.setOnCheckedChangeListener(MainActivity.this);
              runOnUiThread(() ->
                      forceAPI1.setChecked(!CameraBaseAPIClass
                              .getPreferCamera2(getApplicationContext())));
            }
    );
  }

  @Override
  protected void onStart() {
    super.onStart();

    threadPoolExecutor.execute(() -> {
      if (requestDeviceAdminLock != null)
        requestDeviceAdminLock.lock();
      DeviceAdminHelper.requestPermission(MainActivity.this); //todo
      checkUpdate();
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "onDestroy: ");
  }

  @Override
  protected void onActivityResult(@RequestCodes int requestCode,
                                  int resultCode,
                                  @Nullable final Intent data) {
    if (requestCode == RESULT_CANCELED &&
            resultCode == REQUEST_CODE_DEVICE_ADMIN)
      DeviceAdminHelper.onRequestPermissionFinished(this); //todo
  }

  @Override
  public void onRequestPermissionsResult(@RequestCodes int requestCode,
                                         @NonNull final String[] permissions,
                                         @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL)
      CameraBaseAPIClass.onRequestPermissionFinished(
              getApplicationContext(), grantResults);
  }

  @Override
  public void onClick(@NonNull final View view) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "onClick: ");

    threadPoolExecutor.execute(() -> {
      final int id = view.getId();
      final Intent intent = new Intent(MainActivity.this,
              ForegroundService.class);

      if (id == R.id.activity_main_button_stopService)
        stopService(intent);

      else if (CameraBaseAPIClass.requestPermissions(MainActivity.this)) {
        intent.setAction(ACTION_CAPTURE_IMAGE);
        intent.putExtra(EXTRA_CAMERA_FACING,
                id == R.id.activity_main_button_front);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
          startService(intent);
        else
          startForegroundService(intent);
      }
    });
  }

  @Override
  public void onCheckedChanged(@NonNull final CompoundButton buttonView,
                               boolean isChecked) {
    if (buttonView.getId() == R.id.activity_main_api1Switch)
      getSharedPreferences(CameraBaseAPIClass.SP_NAME_CAMERA,
              Context.MODE_PRIVATE).edit()
              .putBoolean(SP_KEY_PREFER_CAMERA_API_2, !isChecked)
              .apply();
  }

  @WorkerThread
  private void checkUpdate() {
    URLConnectionBuilder connectionBuilder = null;
    try {
      final String
              URL = "https://api.github.com/repos/jerryc05/UnlockMe/tags";
      connectionBuilder = URLConnectionBuilder
              .get(URL)
              .setConnectTimeout(1000)
              .setReadTimeout(1000)
              .setUseCache(false)
              .connect(getApplicationContext());

      final String latest = new JSONArray(connectionBuilder.getResult())
              .getJSONObject(0)
              .getString("name").substring(1);

      if (!latest.equals(BuildConfig.VERSION_NAME)) {
        if (BuildConfig.DEBUG)
          Log.d(TAG, "checkUpdate: " + latest);

        final String tagURL = "https://github.com/jerryc05/UnlockMe/releases/tag/v";
        final DialogInterface.OnClickListener positive = (dialogInterface, i) -> {
          dialogInterface.dismiss();
          startActivity(new Intent(Intent.ACTION_VIEW,
                  Uri.parse(tagURL + latest)));
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("New Version Available")
                .setMessage("Do you want to upgrade from\n\tv" +
                        BuildConfig.VERSION_NAME + "  to  v" + latest + '?')
                .setPositiveButton("YES", positive)
                .setNegativeButton("NO", null)
                .setIcon(R.drawable.ic_round_info);

        runOnUiThread(builder::show);
      }

    } catch (final UnknownHostException e) {
      UserInterface.showExceptionToNotification(getApplicationContext(),
              "Cannot connect to github.com!\n>>> " + e.toString(),
              "checkUpdate()");

    } catch (final Exception e) {
      if (!WIFI_ONLY_EXCEPTION_PROMPT.equals(e.getMessage()))
        UserInterface.showExceptionToNotification(getApplicationContext(),
                e.toString(), "checkUpdate()");

    } finally {
      if (connectionBuilder != null)
        connectionBuilder.disconnect();
    }
  }
}
