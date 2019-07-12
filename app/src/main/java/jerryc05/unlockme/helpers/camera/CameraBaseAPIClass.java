package jerryc05.unlockme.helpers.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.UserInterface;

public abstract class CameraBaseAPIClass {

  private static String[]                        permissions;
  private static DialogInterface.OnClickListener requestPermissionRationaleOnClickListener;

  @SuppressWarnings("unused")
  public static void getImageFromDefaultCamera(final MainActivity activity,
                                               final boolean isFront) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getImageFromCamera2(activity, isFront);
    else
      getImageFromCamera1(activity, isFront);
  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera1(final MainActivity activity,
                                         final boolean isFront) {
    requestPermissions(activity);

  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera2(final MainActivity activity,
                                         final boolean isFront) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      final String ERROR_MSG =
              "Cannot use Camera2 API on devices lower than Lollipop";
      UserInterface.throwExceptionToNotification(ERROR_MSG,
              "CameraBaseAPIClass#getImageFromCamera2()");
      throw new UnsupportedOperationException(ERROR_MSG);
    }

    requestPermissions(activity);
    Camera2APIHelper.getImage(activity, isFront
            ? CameraCharacteristics.LENS_FACING_FRONT
            : CameraCharacteristics.LENS_FACING_BACK);
  }

  static boolean requestPermissions(final MainActivity activity) {
    if (!Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()))
      UserInterface.throwExceptionToDialog(activity,
              new UnsupportedOperationException(
                      "requestPermissions() External storage not writable!"));

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (activity.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED))
      return true;

    if (activity.shouldShowRequestPermissionRationale(
            Manifest.permission.CAMERA) ||
            activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Permission Required")
                  .setMessage("We need the following permissions to work properly:\n\n" +
                          "-\t\tCAMERA\n-\t\tWRITE_EXTERNAL_STORAGE")
                  .setIcon(R.drawable.ic_round_warning_24px)
                  .setCancelable(false)
                  .setPositiveButton("OK",
                          getRequestPermissionRationaleOnClickListener(activity))
                  .show();
        }
      });
    } else
      activity.requestPermissions(getPermissionsArray(),
              MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);

    return false;
  }

  @SuppressWarnings("WeakerAccess")
  static DialogInterface.OnClickListener getRequestPermissionRationaleOnClickListener(
          final MainActivity activity) {
    if (requestPermissionRationaleOnClickListener == null)
      requestPermissionRationaleOnClickListener =
              new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialogInterface,
                                    int i) {
                  activity.requestPermissions(getPermissionsArray(),
                          MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);
                }
              };
    return requestPermissionRationaleOnClickListener;
  }

  @SuppressWarnings("WeakerAccess")
  static String[] getPermissionsArray() {
    if (permissions == null)
      permissions = new String[]{
              Manifest.permission.CAMERA,
              Manifest.permission.WRITE_EXTERNAL_STORAGE};
    return permissions;
  }

  public static void onRequestPermissionFinished(final MainActivity activity,
                                                 final int[] grantResults) {
    final boolean granted = grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED;
    final String granted_str = granted
            ? "Camera and Write External Storage Permissions Granted √"
            : "Camera or Write External Storage Permissions Denied ×";
    Toast.makeText(activity, granted_str, Toast.LENGTH_SHORT).show();
  }
}
