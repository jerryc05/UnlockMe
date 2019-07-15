package jerryc05.unlockme.helpers.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.UserInterface;

import static jerryc05.unlockme.activities.MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL;

public abstract class CameraBaseAPIClass {

  public static final String
          SP_NAME_CAMERA             = "CAMERA",
          SP_KEY_PREFER_CAMERA_API_2 = "prefer_camera_api_2";

  private static final boolean canUseCamera2 =
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  public static        boolean isFront       = true;

  @SuppressWarnings("unused")
  public static void getImageFromDefaultCamera(final Context context,
                                               final boolean isFront) {
    CameraBaseAPIClass.isFront = isFront;
    if (getPreferCamera2(context) && canUseCamera2)
      getImageFromCamera2(isFront, context);
    else
      getImageFromCamera1(isFront, context);
  }

  public static boolean getPreferCamera2(final Context context) {
    return context.getSharedPreferences(
            SP_NAME_CAMERA, Context.MODE_PRIVATE)
            .getBoolean(SP_KEY_PREFER_CAMERA_API_2, true);
  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera1(final boolean isFront,
                                         final Context context) {
    if (requestPermissions(context))
      Camera1APIHelper.getImage(isFront
              ? Camera.CameraInfo.CAMERA_FACING_FRONT
              : Camera.CameraInfo.CAMERA_FACING_BACK, context);
  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera2(final boolean isFront,
                                         final Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      final String ERROR_MSG =
              "Cannot use Camera2 API on devices lower than Lollipop";
      UserInterface.showExceptionToNotification(ERROR_MSG,
              "CameraBaseAPIClass#getImageFromCamera2()", context);
      throw new UnsupportedOperationException(ERROR_MSG);
    }

    if (requestPermissions(context))
      Camera2APIHelper.getImage(isFront
              ? CameraCharacteristics.LENS_FACING_FRONT
              : CameraCharacteristics.LENS_FACING_BACK, context);
  }

  public static boolean requestPermissions(final Context context) {
    if (!context.getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      UserInterface.showExceptionToDialog(context,
              new UnsupportedOperationException(
                      "requestPermissions() Camera device not found!"));
    }

    if (!Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()))
      UserInterface.showExceptionToDialog(context,
              new UnsupportedOperationException(
                      "requestPermissions() External storage not writable!"));

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED))
      return true;
    if (context instanceof Activity)
      if (((Activity) context).shouldShowRequestPermissionRationale(
              Manifest.permission.CAMERA) ||
              ((Activity) context).shouldShowRequestPermissionRationale(
                      Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

        ((Activity) context).runOnUiThread(new Runnable() {
          @Override
          public void run() {
            new AlertDialog.Builder(context)
                    .setTitle("Permission Required")
                    .setMessage("We need the following permissions to work properly:\n\n" +
                            "-\t\tCAMERA\n-\t\tWRITE_EXTERNAL_STORAGE")
                    .setIcon(R.drawable.ic_round_warning_24px)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            getRequestPermissionRationaleOnClickListener(
                                    ((Activity) context)))
                    .show();
          }
        });
      } else
        ((Activity) context).requestPermissions(getPermissionsArray(),
                REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);

    return false;
  }

  @SuppressWarnings("WeakerAccess")
  static DialogInterface.OnClickListener getRequestPermissionRationaleOnClickListener(
          final Activity activity) {
    return new DialogInterface.OnClickListener() {
      @SuppressLint("NewApi")
      @Override
      public void onClick(DialogInterface dialogInterface,
                          int i) {
        activity.requestPermissions(getPermissionsArray(),
                REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);
      }
    };
  }

  @SuppressWarnings("WeakerAccess")
  static String[] getPermissionsArray() {
    return new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
  }

  public static void onRequestPermissionFinished(final Activity activity,
                                                 final int[] grantResults) {
    boolean granted = true;
    if (grantResults.length > 0)
      for (int result : grantResults)
        if (result != PackageManager.PERMISSION_GRANTED) {
          granted = false;
          break;
        }

    final String granted_str = granted
            ? "Camera and Write External Storage Permissions Granted √"
            : "Camera or Write External Storage Permissions Denied ×";
    Toast.makeText(activity, granted_str, Toast.LENGTH_SHORT).show();
    if (granted)
      getImageFromDefaultCamera(activity, isFront);
  }

  static void saveImageToDisk(final byte[] data, final Context context) {
    //noinspection SpellCheckingInspection
    final String
            timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",
            Locale.getDefault()).format(new Date()),
            dirName = Environment.getExternalStorageDirectory() + "/UnlockMe/",
            fileName = "UnlockMe_" + timeFormat + ".jpg";

    UserInterface.notifyPictureToUI(fileName, data, context);
    final File
            dir = new File(dirName),
            file = new File(dir, fileName);

    if (!dir.isDirectory() && !dir.mkdirs())
      UserInterface.showExceptionToNotification(
              "Cannot create path " + fileName,
              "saveImageToDisk()", context);

    try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      fileOutputStream.write(data);
    } catch (final Exception e) {
      UserInterface.showExceptionToNotification(
              e.toString(), "saveImageToDisk()", context);
    }
  }
}
