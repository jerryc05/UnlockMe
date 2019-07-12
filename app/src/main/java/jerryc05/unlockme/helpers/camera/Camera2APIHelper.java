package jerryc05.unlockme.helpers.camera;

import android.annotation.SuppressLint;
import android.content.Context;
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

import androidx.annotation.RequiresApi;

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
import jerryc05.unlockme.helpers.UserInterface;

@SuppressWarnings({"NullableProblems", "WeakerAccess"})
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
abstract class Camera2APIHelper extends CameraBaseAPIClass {

  static final   String                               TAG =
          Camera2APIHelper.class.getSimpleName();
  private static int                                  predefinedFacing;
  static         String                               cameraID;
  static         CameraManager                        mCameraManager;
  static         CameraDevice                         mCameraDevice;
  private static CameraCharacteristics                mCameraCharacteristics;
  static         CameraCaptureSession                 mCameraCaptureSession;
  static         CameraDevice.StateCallback           openCameraStateCallback;
  private static SparseIntArray                       orientationsMap;
  private static ImageReader                          previewImageReader;
  private static ImageReader                          mImageReader;
  private static CameraCaptureSession.CaptureCallback mCaptureCallback;
  private static ImageReader.OnImageAvailableListener onImageAvailableListener;
  private static CameraCaptureSession.StateCallback   mStateCallback;

  public static void getImage(final MainActivity activity, final int _facing) {
    predefinedFacing = _facing;
    if (requestPermissions(activity)) {
      setupCamera2(activity);
      openCamera2AndCapture(activity);
    }
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
      UserInterface.throwExceptionToDialog(activity, e);
    }
    if (cameraID == null)
      UserInterface.throwExceptionToDialog(activity,
              new UnsupportedOperationException(
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
          UserInterface.throwExceptionToDialog(activity,
                  new UnsupportedOperationException(
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
            UserInterface.throwExceptionToDialog(activity, e);
          }
        }
      });
  }

  static void captureStillImage(final MainActivity activity) {
    if (mCameraDevice == null)
      UserInterface.throwExceptionToDialog(activity,
              new UnsupportedOperationException(
                      "captureStillImage() found null camera device!"));
    try {
      mCameraDevice.createCaptureSession(Collections.singletonList(
              getCaptureImageReader().getSurface()),
              getCaptureStillImageStateCallback(activity), null);
    } catch (final CameraAccessException e) {
      UserInterface.throwExceptionToDialog(activity, e);
    }
  }

  static ImageReader getPreviewImageReader() {
    if (previewImageReader == null)
      previewImageReader = ImageReader.newInstance(20, 30,
              ImageFormat.JPEG, 1);
    return previewImageReader;
  }

  static ImageReader getCaptureImageReader() {
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
                    getCaptureCallback(activity), null);

          } catch (final Exception e) {
            UserInterface.throwExceptionToDialog(activity, e);
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          UserInterface.throwExceptionToDialog(activity,
                  new UnsupportedOperationException(
                          "CameraCaptureSession#StateCallBack() returns onConfigureFailed()"));
        }
      };
    return mStateCallback;
  }

  static CaptureRequest getStillImageCaptureRequest(
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
    mCaptureRequestBuilder.addTarget(getCaptureImageReader().getSurface());
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

  static CameraCaptureSession.CaptureCallback getCaptureCallback(
          MainActivity activity) {
    if (mCaptureCallback == null)
      mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);

          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureCallback#onCaptureCompleted: ");

//          final CaptureRequest.Builder mCaptureRequestBuilder;
//          try {
//            mCaptureRequestBuilder = mCameraDevice
//                    .createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
//            mCaptureRequestBuilder.set(
//                    CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_OFF
//            );
//            mCaptureRequestBuilder.set(
//                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
//                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
//            );
//            mCaptureRequestBuilder.set(
//                    CaptureRequest.CONTROL_AF_MODE,
//                    CameraMetadata.CONTROL_AF_MODE_OFF
//            );
//            mCaptureRequestBuilder.set(
//                    CaptureRequest.CONTROL_AF_TRIGGER,
//                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
//            );
////            mCaptureRequestBuilder.addTarget(getCaptureImageReader().getSurface());
//            mCaptureRequestBuilder.addTarget(getPreviewImageReader().getSurface());
//
//            session.capture(mCaptureRequestBuilder.build(),
//                    null, null);
//          } catch (Exception e) {
//            UserInterface.throwExceptionToDialog(activity, e);
//          }
          closeCamera2();
        }
      };
    return mCaptureCallback;
  }

  static void saveImageToDisk(final byte[] bytes) {
    final String
            timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",
            Locale.getDefault()).format(new Date()),
            dirName = Environment.getExternalStorageDirectory() + "/UnlockMe/",
            fileName = "UnlockMe_" + timeFormat + ".jpg";

    if (BuildConfig.DEBUG)
      Log.d(TAG, "saveImageToDisk: " + dirName + fileName);

    UserInterface.notifyPictureToUI(fileName, bytes);
    final File
            dir = new File(dirName),
            file = new File(dir, fileName);

    if (!dir.isDirectory() && !dir.mkdirs())
      UserInterface.throwExceptionToNotification("Cannot create path " + fileName,
              "saveImageToDisk()");

    try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      fileOutputStream.write(bytes);
    } catch (final Exception e) {
      if (BuildConfig.DEBUG)
        Log.e(TAG, "saveImageToDisk: ", e);
      UserInterface.throwExceptionToNotification(e.toString(), "saveImageToDisk()");
    }
  }

  static void closeCamera2() {
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
}
