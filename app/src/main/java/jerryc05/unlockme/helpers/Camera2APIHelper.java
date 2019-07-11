package jerryc05.unlockme.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;

import static android.content.Context.NOTIFICATION_SERVICE;

@SuppressWarnings({"NullableProblems", "WeakerAccess"})
public final class Camera2APIHelper {

  static final     String                               TAG =
          Camera2APIHelper.class.getSimpleName();
  private static   String[]                             permissions;
  protected static String                               cameraID;
  protected static CameraManager                        mCameraManager;
  protected static CameraDevice                         mCameraDevice;
  private static   CameraCharacteristics                mCameraCharacteristics;
  protected static CameraCaptureSession                 mCameraCaptureSession;
  protected static CameraDevice.StateCallback           openCameraStateCallback;
  private static   SparseIntArray                       orientationsMap;
  protected static DialogInterface.OnClickListener      requestPermissionRationaleOnClickListener;
  private static   ImageReader                          mImageReader;
  private static   CameraCaptureSession.CaptureCallback mCaptureCallback;
  private static   CaptureRequest.Builder               mCaptureRequestBuilder;
  private static   ImageReader.OnImageAvailableListener onImageAvailableListener;
  private static   CameraCaptureSession.StateCallback   mStateCallback;

  public static void automaticTakePhoto(final MainActivity activity) {
    if (requestPermissions(activity)) {
      setupCamera2(activity);
      openCamera2AndCapture(activity);
    }
  }

  private static boolean requestPermissions(final MainActivity activity) {
    if (!Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()))
      activity.alertExceptionToUI(new UnsupportedOperationException(
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
      if (requestPermissionRationaleOnClickListener == null)
        requestPermissionRationaleOnClickListener =
                new DialogInterface.OnClickListener() {
                  @SuppressLint("NewApi")
                  @Override
                  public void onClick(DialogInterface dialogInterface,
                                      int i) {
                    activity.requestPermissions(getPermissionsArray(),
                            MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);
                  }
                };
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
                          requestPermissionRationaleOnClickListener)
                  .show();
        }
      });
    } else
      activity.requestPermissions(getPermissionsArray(),
              MainActivity.REQUEST_CODE_CAMERA_AND_WRITE_EXTERNAL);

    return false;
  }

  protected static String[] getPermissionsArray() {
    if (permissions == null)
      permissions = new String[]{
              Manifest.permission.CAMERA,
              Manifest.permission.WRITE_EXTERNAL_STORAGE};
    return permissions;
  }

  public static void onRequestPermissionFinished(final MainActivity activity,
                                                 int[] grantResults) {
    final boolean granted = grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED;
    final String granted_str = granted
            ? "Camera and Write External Storage Permissions Granted √"
            : "Camera or Write External Storage Permissions Denied ×";

    Toast.makeText(activity, granted_str, Toast.LENGTH_SHORT).show();
    if (granted) Camera2APIHelper.automaticTakePhoto(activity);
  }

  private static void setupCamera2(final MainActivity activity) {
    getCameraManager(activity);
    getCameraIDAndCharacteristics(activity);
    getOpenCameraStateCallback(activity);
  }

  private static void getCameraManager(final MainActivity activity) {
    if (mCameraManager == null)
      mCameraManager = (CameraManager)
              activity.getSystemService(Context.CAMERA_SERVICE);
  }

  private static void getCameraIDAndCharacteristics(final MainActivity activity) {
    try {
      assert mCameraManager != null;
      for (String eachCameraID : mCameraManager.getCameraIdList()) {

        CameraCharacteristics eachCameraCharacteristics =
                mCameraManager.getCameraCharacteristics(eachCameraID);

        final Integer FACING = eachCameraCharacteristics.get(
                CameraCharacteristics.LENS_FACING);
        if (FACING != null &&
                FACING == CameraCharacteristics.LENS_FACING_FRONT) {
          cameraID = eachCameraID;
          mCameraCharacteristics = eachCameraCharacteristics;
          break;
        }
      }
    } catch (final Exception e) {
      activity.alertExceptionToUI(e);
    }
    if (cameraID == null)
      activity.alertExceptionToUI(new UnsupportedOperationException(
              "getCameraIDAndCharacteristics() cannot find Front Camera!"));
  }

  private static void getOpenCameraStateCallback(final MainActivity activity) {
    if (openCameraStateCallback == null)
      openCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
          mCameraDevice = cameraDevice;
          captureStillImage(activity);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
          cameraDevice.close();
          mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
          onDisconnected(cameraDevice);
          activity.alertExceptionToUI(new UnsupportedOperationException(
                  "openCameraStateCallback#onError() returns error code: "
                          + error + '!'));
        }
      };
  }

  private static void openCamera2AndCapture(final MainActivity activity) {
    if (requestPermissions(activity))
      activity.runOnUiThread(new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
          try {
            if (BuildConfig.DEBUG)
              Log.d(TAG, "openCamera2AndCapture: ");
            mCameraManager.openCamera(cameraID, openCameraStateCallback, null);
          } catch (final CameraAccessException e) {
            activity.alertExceptionToUI(e);
          }
        }
      });
  }

  protected static void captureStillImage(final MainActivity activity) {
    if (mCameraDevice == null)
      activity.alertExceptionToUI(new UnsupportedOperationException(
              "captureStillImage() found null camera device!"));
    try {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "captureStillImage: ");
      mCameraDevice.createCaptureSession(Collections.singletonList(
              getImageReader().getSurface()),
              getCaptureStillImageStateCallback(activity)
              , null);
    } catch (final CameraAccessException e) {
      activity.alertExceptionToUI(e);
    }
  }

  private static ImageReader getImageReader() {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "getImageReader: ");

    if (mImageReader == null) {
      final StreamConfigurationMap streamConfigurationMap =
              mCameraCharacteristics.get(
                      CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      assert streamConfigurationMap != null;
      final Size mCaptureSize = Collections.max(Arrays.asList(
              streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
              new Comparator<Size>() {
                @Override
                public int compare(Size prev, Size next) {
                  return prev.getWidth() - next.getWidth();
                }
              });
      mImageReader = ImageReader.newInstance(
              mCaptureSize.getWidth(), mCaptureSize.getHeight(),
              ImageFormat.JPEG, 1);
      mImageReader.setOnImageAvailableListener(
              getOnImageAvailableListener(), null);
    }
    return mImageReader;
  }

  private static CameraCaptureSession.StateCallback getCaptureStillImageStateCallback(
          MainActivity activity) {
    if (mStateCallback == null)
      mStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureStillImageStateCallback#onConfigured: ");
          try {
            mCameraCaptureSession = cameraCaptureSession;
            cameraCaptureSession.capture(getCaptureRequest(activity),
                    getCaptureCallback(), null);
          } catch (final CameraAccessException e) {
            activity.alertExceptionToUI(e);
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          activity.alertExceptionToUI(new UnsupportedOperationException(
                  "CameraCaptureSession#StateCallBack() returns onConfigureFailed()"));
        }
      };
    return mStateCallback;
  }

  protected static CaptureRequest getCaptureRequest(MainActivity activity)
          throws CameraAccessException {
    if (mCaptureRequestBuilder == null) {
      mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(
              CameraDevice.TEMPLATE_STILL_CAPTURE);
      mCaptureRequestBuilder.addTarget(getImageReader().getSurface());
      mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE,
              CameraMetadata.CONTROL_MODE_AUTO);
    }
    mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
            getOrientationsMap().get(activity.getWindowManager()
                    .getDefaultDisplay().getRotation()));
    return mCaptureRequestBuilder.build();
  }

  private static SparseIntArray getOrientationsMap() {
    if (orientationsMap == null) {
      orientationsMap = new SparseIntArray(4);
      orientationsMap.append(Surface.ROTATION_0, 90);
      orientationsMap.append(Surface.ROTATION_90, 0);
      orientationsMap.append(Surface.ROTATION_180, 270);
      orientationsMap.append(Surface.ROTATION_270, 180);
    }
    return orientationsMap;
  }

  private static ImageReader.OnImageAvailableListener getOnImageAvailableListener() {
    if (onImageAvailableListener == null)
      onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mOnImageAvailableListener#onImageAvailable: ");

          final Image      image  = imageReader.acquireLatestImage();
          final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
          final byte[]     bytes  = new byte[buffer.capacity()];
          buffer.get(bytes);
          saveImageToDisk(bytes);
          image.close();
        }
      };
    return onImageAvailableListener;
  }

  protected static CameraCaptureSession.CaptureCallback getCaptureCallback() {
    if (mCaptureCallback == null)
      mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureCallback#onCaptureCompleted: ");

          closeCamera2();
        }
      };
    return mCaptureCallback;
  }

  protected static void saveImageToDisk(final byte[] bytes) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "saveImageToDisk: ");

    final String timeFormat = new SimpleDateFormat(
            "yyyy-MM-dd_HH_mm_ss", Locale.getDefault())
            .format(new Date());
    final File file = new File(Environment.getExternalStorageDirectory() +
            "/UnlockMe/UnlockMe_" + timeFormat + ".jpg");
    try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      fileOutputStream.write(bytes);
      notifyToUI("Picture Taken", "Picture taken successfully",
              timeFormat);
    } catch (final Exception e) {
      if (BuildConfig.DEBUG)
        Log.e(TAG, "saveImageToDisk: ", e);
      notifyExceptionToUI(e.toString(), "saveImageToDisk()");
    }
  }

  protected static void closeCamera2() {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "closeCamera2: ");

    if (mCameraCaptureSession != null) {
      mCameraCaptureSession.close();
      mCameraCaptureSession = null;
    }
    if (mCameraDevice != null) {
      mCameraDevice.close();
      mCameraDevice = null;
    }
    if (mImageReader != null) {
      mImageReader.close();
      mImageReader = null;
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void notifyExceptionToUI(String contentText, String subText) {
    notifyToUI("Crash Report", contentText, subText);
  }

  private static void notifyToUI(String contentTitle, String contentText, String subText) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSubText(subText);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      builder.setChannelId("Crash Report");
    NotificationManager notificationManager = (NotificationManager)
            MainActivity.applicationContext.
                    getSystemService(NOTIFICATION_SERVICE);
    assert notificationManager != null;
    notificationManager.notify(-1, builder.build());
  }
}
