package jerryc05.unlockme.helpers.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.annotation.WorkerThread;

import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.R;
import jerryc05.unlockme.helpers.UserInterface;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static jerryc05.unlockme.activities.MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL;

@WorkerThread
public abstract class CameraBaseAPIClass {

  public static final String
          SP_NAME_CAMERA             = "CAMERA",
          SP_KEY_PREFER_CAMERA_API_2 = "prefer_camera_api_2",
          EXTRA_CAMERA_FACING        = "EXTRA_CAMERA_FACING";

  private static final String
          TAG = "CameraBaseAPIClass";

  private static final boolean canUseCamera2 =
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  private static       boolean isFront       = true;
  @SuppressWarnings("CanBeFinal")
  static               int     imageCount    = 5;

  @SuppressWarnings("unused")
  @StringDef(SP_NAME_CAMERA)
  @Retention(RetentionPolicy.SOURCE)
  private @interface SharedPreferenceNames {
  }

  @SuppressWarnings("unused")
  @StringDef(SP_KEY_PREFER_CAMERA_API_2)
  @Retention(RetentionPolicy.SOURCE)
  private @interface SharedPreferenceKeys {
  }

  @SuppressWarnings("unused")
  @StringDef(EXTRA_CAMERA_FACING)
  @Retention(RetentionPolicy.SOURCE)
  public @interface IntentExtraKeys {
  }

  @SuppressWarnings("unused")
  public static void getImageFromDefaultCamera(
          @NonNull final Context context,
          final boolean isFront) {
    CameraBaseAPIClass.isFront = isFront;
    if (getPreferCamera2(context) && canUseCamera2)
      getImageFromCamera2(isFront, context);
    else
      getImageFromCamera1(isFront, context);
  }

  public static boolean getPreferCamera2(
          @NonNull final Context context) {
    return context.getSharedPreferences(
            SP_NAME_CAMERA, Context.MODE_PRIVATE)
            .getBoolean(SP_KEY_PREFER_CAMERA_API_2, true);
  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera1(
          final boolean isFront,
          @NonNull final Context context) {
    if (requestPermissions(context))
      Camera1APIHelper.getImage(isFront
              ? Camera.CameraInfo.CAMERA_FACING_FRONT
              : Camera.CameraInfo.CAMERA_FACING_BACK, context);
  }

  @SuppressWarnings("WeakerAccess")
  public static void getImageFromCamera2(
          final boolean isFront,
          @NonNull final Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      final String ERROR_MSG =
              "Cannot use Camera2 API on devices lower than Lollipop";
      UserInterface.throwExceptionAsNotification(ERROR_MSG,
              "CameraBaseAPIClass#getImageFromCamera2()", context);
      throw new UnsupportedOperationException(ERROR_MSG);
    }

    if (requestPermissions(context))
      Camera2APIHelper.getImage(isFront
              ? CameraCharacteristics.LENS_FACING_FRONT
              : CameraCharacteristics.LENS_FACING_BACK, context);
  }

  public static boolean requestPermissions(
          @NonNull final Context context) {
//    if (!context.getPackageManager()
//            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
//      if (BuildConfig.DEBUG)
//        Log.d(TAG, "requestPermissions: " + Arrays.toString(
//                context.getPackageManager().getSystemAvailableFeatures()));
//
//      UserInterface.throwExceptionAsDialog(new UnsupportedOperationException(
//              "requestPermissions() Camera device not found!"), context);
//    }

    if (!Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()))
      UserInterface.throwExceptionAsDialog(new UnsupportedOperationException(
              "requestPermissions() External storage not writable!"), context);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PERMISSION_GRANTED && context.checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PERMISSION_GRANTED))
      return true;

    if (BuildConfig.DEBUG)
      Log.d(TAG, "requestPermissions: No permission!");

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
                    .setMessage("We need the following permissions to work properly:\n" +
                            "\n-\t\tCAMERA\n-\t\tWRITE_EXTERNAL_STORAGE")
                    .setIcon(R.drawable.ic_round_warning)
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
  static OnClickListener getRequestPermissionRationaleOnClickListener(
          @NonNull final Activity activity) {
    return new OnClickListener() {
      @SuppressLint("NewApi")
      @Override
      public void onClick(@NonNull final DialogInterface dialogInterface,
                          int i) {
        dialogInterface.dismiss();
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

  public static void onRequestPermissionFinished(
          @NonNull final Context context,
          @NonNull final int[] grantResults) {
    boolean granted = true;
    if (grantResults.length > 0)
      for (int result : grantResults)
        if (result != PERMISSION_GRANTED) {
          granted = false;
          break;
        }

    final String granted_str = granted
            ? "Camera and Write External Storage Permissions Granted √"
            : "Camera or Write External Storage Permissions Denied ×";
    Toast.makeText(context, granted_str, Toast.LENGTH_SHORT).show();

    if (granted)
      getImageFromDefaultCamera(context, isFront);
  }

  static void saveImageToDisk(@NonNull final byte[] data,
                              @NonNull final Context context) {
    //noinspection SpellCheckingInspection
    final String
            timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",
            Locale.getDefault()).format(new Date()),
            fileName = "UnlockMe_" + timeFormat + ".jpg";

    // todo folder name in /picture

    UserInterface.notifyPictureToUI(fileName, data, context);

    final ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // todo file name
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      values.put(MediaStore.Images.Media.IS_PENDING, 1);

    final ContentResolver resolver = context.getContentResolver();
    final Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            item = Objects.requireNonNull(resolver.insert(external, values));

    try (final OutputStream outputStream = resolver.openOutputStream(item)) {
      Objects.requireNonNull(outputStream).write(data);
    } catch (final Exception e) {
      UserInterface.throwExceptionAsNotification(e.toString(),
              "saveImageToDisk()", context);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      values.clear();
      values.put(MediaStore.Images.Media.IS_PENDING, 0);
      resolver.update(item, values, null, null);
    }
  }

  public static void trimMemory() {
    Camera1APIHelper.trimMemory();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      Camera2APIHelper.trimMemory();

  }
}
