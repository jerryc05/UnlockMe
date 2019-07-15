package jerryc05.unlockme.helpers;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.activities.MainActivity;
import jerryc05.unlockme.R;
import jerryc05.unlockme.receivers.MyDeviceAdminReceiver;

import static android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION;
import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN;
import static jerryc05.unlockme.activities.MainActivity.REQUEST_CODE_DEVICE_ADMIN;

public final class DeviceAdminHelper {

  private static final String        deviceAdminPermissionExplanation =
          "We need DEVICE ADMIN permission to work properly.";
  private static       ComponentName mComponentName;

  public static void requestPermission(MainActivity activity) {
    if (activity.requestDeviceAdminLock != null) {
      activity.requestDeviceAdminLock.unlock();
      activity.requestDeviceAdminLock = null;
    }

    if (!getDevicePolicyManager(activity).isAdminActive(
            getComponentName(activity))) {
      final Intent intentDeviceAdmin = new Intent(
              DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
      intentDeviceAdmin.putExtra(EXTRA_DEVICE_ADMIN,
              mComponentName);
      intentDeviceAdmin.putExtra(EXTRA_ADD_EXPLANATION,
              deviceAdminPermissionExplanation);

      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.startActivityForResult(intentDeviceAdmin,
                  REQUEST_CODE_DEVICE_ADMIN);
        }
      });
    } else
      mComponentName = null;
  }

  public static void onRequestPermissionFinished(MainActivity activity) {
    if (!getDevicePolicyManager(activity).isAdminActive(
            getComponentName(activity))) {
      if (activity.requestDeviceAdminLock == null)
        activity.requestDeviceAdminLock = new ReentrantLock();
      activity.requestDeviceAdminLock.lock();

      final DialogInterface.OnClickListener onClickListener =
              new DialogInterface.OnClickListener() {
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
    } else
      mComponentName = null;
  }

  private static DevicePolicyManager getDevicePolicyManager(final Context context) {
    return (DevicePolicyManager) Objects.requireNonNull(
           context.getSystemService(Context.DEVICE_POLICY_SERVICE));
  }

  private static ComponentName getComponentName(final Context context) {
    if (mComponentName == null)
      mComponentName = new ComponentName(context, MyDeviceAdminReceiver.class);
    return mComponentName;
  }
}
