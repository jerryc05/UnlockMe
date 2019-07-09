package jerryc05.unlockme;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import jerryc05.unlockme.receivers.MyDAReceiver;

@SuppressWarnings("NullableProblems")
public class MainActivity extends Activity {

  private final static String              TAG                 =
          MainActivity.class.getName();
  private              DevicePolicyManager devicePolicyManager;
  private              ComponentName       componentName;
  private final static int                 REQUEST_CODE_CAMERA = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    new Thread(new Runnable() {
      @Override
      public void run() {
        handleDeviceAdmin();
        findViewById(R.id.activity_main_button_takePhoto)
                .setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                    takePhoto();
                  }
                });
      }
    }).start();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (isDeviceAdminDisabled())
      requireDeviceAdmin();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      final String granted = grantResults.length > 0 &&
              grantResults[0] == PackageManager.PERMISSION_GRANTED
              ? "Camera Permission Granted √"
              : "Camera Permission Denied ×";
      if (BuildConfig.DEBUG)
        Log.d(TAG, "onRequestPermissionsResult: " + granted);
      Toast.makeText(this,
              granted, Toast.LENGTH_SHORT)
              .show();
      takePhoto();
    }
  }

  void takePhoto() {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "takePhoto: ");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
      if (shouldShowRequestPermissionRationale(
              Manifest.permission.CAMERA))
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage(getResources().getString(R.string.permission_camera_explanation))
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    requestPermissions(
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CODE_CAMERA);
                  }
                })
                .show();
      else
        requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA);

    } else {
      final int REQUEST_IMAGE_CAPTURE = 1;

      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      }
    }
  }

  void handleDeviceAdmin() {
    devicePolicyManager = (DevicePolicyManager)
            getSystemService(Context.DEVICE_POLICY_SERVICE);
    componentName = new ComponentName(
            getApplicationContext(), MyDAReceiver.class);

    if (isDeviceAdminDisabled())
      requireDeviceAdmin();
  }

  private boolean isDeviceAdminDisabled() {
    final boolean disabled = devicePolicyManager == null ||
            !devicePolicyManager.isAdminActive(componentName);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this,
                disabled ? "Device Admin Disabled ×"
                        : "Device Admin Enabled √", Toast.LENGTH_SHORT)
                .show();
      }
    });
    return disabled;
  }

  private void requireDeviceAdmin() {
    Intent intent = new Intent(
            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            componentName);
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            getResources().getString(R.string.device_admin_explanation));

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        startActivity(intent);
      }
    });
  }
}
