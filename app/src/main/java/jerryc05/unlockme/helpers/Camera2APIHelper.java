package jerryc05.unlockme.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
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
  private static   int                                  predefinedFacing;
  private static   String[]                             permissions;
  protected static String                               cameraID;
  protected static CameraManager                        mCameraManager;
  protected static CameraDevice                         mCameraDevice;
  private static   CameraCharacteristics                mCameraCharacteristics;
  protected static CameraCaptureSession                 mCameraCaptureSession;
  protected static CameraDevice.StateCallback           openCameraStateCallback;
  private static   SparseIntArray                       orientationsMap;
  protected static DialogInterface.OnClickListener      requestPermissionRationaleOnClickListener;
  private static   ImageReader                          previewImageReader;
  private static   ImageReader                          mImageReader;
  private static   CameraCaptureSession.CaptureCallback mCaptureCallback;
  private static   ImageReader.OnImageAvailableListener onImageAvailableListener;
  private static   CameraCaptureSession.StateCallback   mStateCallback;

  public static void automaticTakePhoto(final MainActivity activity,
                                        final int _facing) {
    predefinedFacing = _facing;
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
                                                 final int[] grantResults) {
    final boolean granted = grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED;
    final String granted_str = granted
            ? "Camera and Write External Storage Permissions Granted √"
            : "Camera or Write External Storage Permissions Denied ×";

    Toast.makeText(activity, granted_str, Toast.LENGTH_SHORT).show();
    if (granted)
      Camera2APIHelper.automaticTakePhoto(activity, predefinedFacing);
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
        if (FACING != null && FACING == predefinedFacing) {
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
      mCameraDevice.createCaptureSession(Collections.singletonList(
              getImageReader().getSurface()),
              getCaptureStillImageStateCallback(activity), null);
    } catch (final CameraAccessException e) {
      activity.alertExceptionToUI(e);
    }
  }

  protected static ImageReader getPreviewImageReader() {
    if (previewImageReader == null) {
      previewImageReader = ImageReader.newInstance(1, 1,
              ImageFormat.JPEG, 1);
    }
    return previewImageReader;
  }

  protected static ImageReader getImageReader() {
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
                  int width = prev.getWidth() - next.getWidth();
                  return width != 0 ? width :
                          prev.getHeight() - next.getHeight();
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
          final MainActivity activity) {
    if (mStateCallback == null)
      mStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureStillImageStateCallback#onConfigured: ");
          try {
            mCameraCaptureSession = cameraCaptureSession;
            cameraCaptureSession.capture(getStillImageCaptureRequest(activity),
                    getCaptureCallback(), null);

          } catch (final Exception e) {
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

  protected static CaptureRequest getPreviewCaptureRequest(
          final MainActivity activity)
          throws CameraAccessException {
    CaptureRequest.Builder mCaptureRequestBuilder = mCameraDevice
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO);
    mCaptureRequestBuilder.addTarget(ImageReader.newInstance(
            1, 1, ImageFormat.JPEG, 50).getSurface());
    return mCaptureRequestBuilder.build();
  }

  protected static CaptureRequest getStillImageCaptureRequest(
          final MainActivity activity)
          throws CameraAccessException {
    CaptureRequest.Builder mCaptureRequestBuilder = mCameraDevice
            .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
    mCaptureRequestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON
    );
    mCaptureRequestBuilder.set(
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
    );
    mCaptureRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CameraMetadata.CONTROL_AF_MODE_AUTO
    );
    mCaptureRequestBuilder.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_TRIGGER_START
    );
    mCaptureRequestBuilder.addTarget(getImageReader().getSurface());
    mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
            (predefinedFacing == CameraCharacteristics.LENS_FACING_FRONT
                    ? getFrontOrientationsMap()
                    : getBackOrientationsMap()).get(
                    activity.getWindowManager().getDefaultDisplay().getRotation()));

    return mCaptureRequestBuilder.build();
  }

  private static SparseIntArray getOrientationsMap() {
    if (orientationsMap == null) {
      orientationsMap = new SparseIntArray(4);
      orientationsMap.append(Surface.ROTATION_90, 0);
      orientationsMap.append(Surface.ROTATION_270, 180);
    }
    return orientationsMap;
  }

  private static SparseIntArray getFrontOrientationsMap() {
    getOrientationsMap().append(Surface.ROTATION_0, 270);
    orientationsMap.append(Surface.ROTATION_180, 90);
    return orientationsMap;
  }

  private static SparseIntArray getBackOrientationsMap() {
    getOrientationsMap().append(Surface.ROTATION_0, 90);
    orientationsMap.append(Surface.ROTATION_180, 270);
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

          final CaptureRequest.Builder mCaptureRequestBuilder;
          try {
            mCaptureRequestBuilder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
            );
            mCaptureRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
            );
            mCaptureRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
            );
            mCaptureRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            );
            mCaptureRequestBuilder.addTarget(getPreviewImageReader().getSurface());

            session.capture(mCaptureRequestBuilder.build(),
                    null, null);
          } catch (Exception e) {
            e.printStackTrace();
          }
          closeCamera2();
        }
      };
    return mCaptureCallback;
  }

  protected static void saveImageToDisk(final byte[] bytes) {
    final String
            timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",
            Locale.getDefault()).format(new Date()),
            dirName = Environment.getExternalStorageDirectory() + "/UnlockMe/",
            fileName = "UnlockMe_" + timeFormat + ".jpg";

    if (BuildConfig.DEBUG)
      Log.d(TAG, "saveImageToDisk: " + dirName + fileName);

    notifyPictureToUI("Picture Taken", fileName, bytes);
    final File
            dir = new File(dirName),
            file = new File(dir, fileName);

    if (!dir.isDirectory() && !dir.mkdirs())
      notifyExceptionToUI("Cannot create path " + fileName,
              "saveImageToDisk()");

    try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      fileOutputStream.write(bytes);
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
  public static void notifyExceptionToUI(final String contentText,
                                         final String subText) {
    notifyToUI("Crash Report", contentText, subText);
    throw new UnsupportedOperationException(contentText);
  }

  public static void notifyToUI(final String contentTitle,
                                final String contentText,
                                final String subText) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_shield_foreground)
            .setSubText(subText)
            .setShowWhen(true)
            .setStyle(new Notification.BigTextStyle()
                    .bigText(contentText));

    NotificationManager notificationManager = (NotificationManager)
            MainActivity.applicationContext.
                    getSystemService(NOTIFICATION_SERVICE);
    assert notificationManager != null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      final String CHANNEL_ID = "Crash Report";
      if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
        NotificationChannel mChannel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription("Crash report notification channel for UnlockMe");
        mChannel.enableLights(true);
        mChannel.enableVibration(true);
        mChannel.setShowBadge(true);
        mChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(mChannel);
      }
      builder
              .setChannelId(CHANNEL_ID)
              .setBadgeIconType(Notification.BADGE_ICON_LARGE);
    } else
      builder.setPriority(Notification.PRIORITY_HIGH);

    notificationManager.notify(-1, builder.build());
  }

  public static void notifyPictureToUI(final String contentTitle,
                                       final String contentText,
                                       final byte[] bytes) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_shield_foreground)
            .setShowWhen(true)
            .setStyle(new Notification.BigPictureStyle()
                    .bigPicture(BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length)));

    NotificationManager notificationManager = (NotificationManager)
            MainActivity.applicationContext.
                    getSystemService(NOTIFICATION_SERVICE);
    assert notificationManager != null;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      final String CHANNEL_ID = "Image Captured Report";
      if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
        NotificationChannel mChannel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription("Image captured report notification channel for UnlockMe");
        mChannel.enableLights(true);
        mChannel.enableVibration(false);
        mChannel.setShowBadge(true);
        mChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(mChannel);
      }
      builder
              .setChannelId(CHANNEL_ID)
              .setBadgeIconType(Notification.BADGE_ICON_LARGE);
    } else
      builder.setPriority(Notification.PRIORITY_HIGH);

    notificationManager.notify(-1, builder.build());
  }
}
