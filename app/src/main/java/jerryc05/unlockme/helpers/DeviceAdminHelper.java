package jerryc05.unlockme.helpers;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.Objects;

import jerryc05.unlockme.MyDeviceAdminReceiver;
import jerryc05.unlockme.R;
import jerryc05.unlockme.activities.MainActivity;

import static android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION;
import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN;
import static jerryc05.unlockme.activities.MainActivity.REQUEST_CODE_DEVICE_ADMIN;

public final class DeviceAdminHelper {

  public static final String        deviceAdminPermissionExplanation =
          "We need DEVICE ADMIN permission to work properly.";  //todo i18n
  private static      ComponentName mComponentName;

  public static void requestPermission(
          @NonNull final MainActivity activity) {
//    if (activity.mRequestDeviceAdminLock != null) {
//      activity.mRequestDeviceAdminLock.unlock();
//      activity.mRequestDeviceAdminLock = null;
//    }

    if (!getDevicePolicyManager(activity).isAdminActive(
            getComponentName(activity))) {
      final Intent intentDeviceAdmin = new Intent(
              DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
      intentDeviceAdmin.putExtra(EXTRA_DEVICE_ADMIN,
              mComponentName);
      intentDeviceAdmin.putExtra(EXTRA_ADD_EXPLANATION,
              deviceAdminPermissionExplanation);

      activity.runOnUiThread(() ->
              activity.startActivityForResult(
                      intentDeviceAdmin, REQUEST_CODE_DEVICE_ADMIN));
    } else
      mComponentName = null;
  }

  public static void onRequestPermissionFinished(
          @NonNull final MainActivity activity) {
    final Context applicationContext = activity.getApplicationContext();

    if (!getDevicePolicyManager(applicationContext)
            .isAdminActive(getComponentName(applicationContext))) {
//      if (activity.mRequestDeviceAdminLock == null)
//        activity.mRequestDeviceAdminLock = new ReentrantLock();
//      activity.mRequestDeviceAdminLock.lock();

      final DialogInterface.OnClickListener onClickListener =
              (dialogInterface, which) -> {
                dialogInterface.dismiss();
                System.exit(1);
              };

      new AlertDialog.Builder(activity)
              .setTitle("Permission Required")
              .setMessage(deviceAdminPermissionExplanation)
              .setIcon(R.drawable.ic_round_error)
              .setCancelable(false)
              .setPositiveButton("OK", onClickListener)
              .show();
    } else
      mComponentName = null;
  }

  private static DevicePolicyManager getDevicePolicyManager(
          @NonNull final Context context) {
    return (DevicePolicyManager) Objects.requireNonNull(
            context.getSystemService(Context.DEVICE_POLICY_SERVICE));
  }

  private static ComponentName getComponentName(
          @NonNull final Context context) {
    if (mComponentName == null)
      mComponentName = new ComponentName(context, MyDeviceAdminReceiver.class);
    return mComponentName;
  }
}
