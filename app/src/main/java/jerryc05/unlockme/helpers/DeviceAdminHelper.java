package jerryc05.unlockme.helpers;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;
import jerryc05.unlockme.receivers.MyDeviceAdminReceiver;

public final class DeviceAdminHelper {

  private static final String TAG          =
          DeviceAdminHelper.class.getSimpleName(),
          deviceAdminPermissionExplanation =
                  "We need DEVICE ADMIN permission to work properly.";
  private static DevicePolicyManager             mDevicePolicyManager;
  private static ComponentName                   mComponentName;
  private static DialogInterface.OnClickListener onClickListener;

  public static void requestPermission(MainActivity activity) {
    if (activity.requestDeviceAdminLock != null) {
      activity.requestDeviceAdminLock.unlock();
      activity.requestDeviceAdminLock = null;
    }
    if (mDevicePolicyManager == null)
      mDevicePolicyManager = (DevicePolicyManager) Objects.requireNonNull(
              activity.getSystemService(Context.DEVICE_POLICY_SERVICE));
    if (mComponentName == null)
      mComponentName =
              new ComponentName(activity, MyDeviceAdminReceiver.class);

    if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
      final Intent intentDeviceAdmin = new Intent(
              DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
      intentDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
              mComponentName);
      intentDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
              deviceAdminPermissionExplanation);

      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.startActivityForResult(intentDeviceAdmin,
                  MainActivity.REQUEST_CODE_DEVICE_ADMIN);
        }
      });
    } else if (BuildConfig.DEBUG)
      Log.d(TAG, "requestPermission: DA is activated!");
  }

  public static void onRequestPermissionFinished(MainActivity activity) {

    if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
      if (activity.requestDeviceAdminLock == null)
        activity.requestDeviceAdminLock = new ReentrantLock();
      activity.requestDeviceAdminLock.lock();

      if (onClickListener == null)
        onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            if (i == DialogInterface.BUTTON_POSITIVE)
              System.exit(1);
          }
        };

      new AlertDialog.Builder(activity)
              .setTitle("Permission Required")
              .setMessage(deviceAdminPermissionExplanation)
              .setIcon(R.drawable.ic_round_error_24px)
              .setCancelable(false)
              .setPositiveButton("OK", onClickListener)
              .show();
    }
  }
}
