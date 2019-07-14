package jerryc05.unlockme.helpers.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.helpers.UserInterface;

@SuppressWarnings({"NullableProblems", "WeakerAccess"})
@SuppressLint("NewApi")
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
  private static ImageReader                          mImageReader;
  private static CameraCaptureSession.CaptureCallback mCaptureCallback;
  private static ImageReader.OnImageAvailableListener onImageAvailableListener;
  private static CameraCaptureSession.StateCallback   mStateCallback;

  public static void getImage(final int facing) {
    predefinedFacing = facing;
    setupCamera2();
    openCamera2AndCapture();
  }

  private static void setupCamera2() {
    getCameraManager();
    getCameraIDAndCharacteristics();
    getOpenCameraStateCallback();
  }

  private static void getCameraManager() {
    if (mCameraManager == null)
      mCameraManager = (CameraManager)
              MainActivity.applicationContext
                      .getSystemService(Context.CAMERA_SERVICE);
  }

  private static void getCameraIDAndCharacteristics() {
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
      UserInterface.showExceptionToNotification(
              e.toString(), "getCameraIDAndCharacteristics");
    }
    if (cameraID == null)
      UserInterface.showExceptionToNotification(
              "getCameraIDAndCharacteristics() cannot find Front Camera!",
              "getCameraIDAndCharacteristics");
  }

  private static void getOpenCameraStateCallback() {
    if (openCameraStateCallback == null)
      openCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
          mCameraDevice = cameraDevice;
          captureStillImage();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
          cameraDevice.close();
          mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
          onDisconnected(cameraDevice);
          UserInterface.showExceptionToNotification(
                  "openCameraStateCallback#onError() returns error code: "
                          + error + '!', "onError()");
        }
      };
  }

  @SuppressLint("MissingPermission")
  private static void openCamera2AndCapture() {
    final MainActivity activity = MainActivity.weakMainActivity.get();
    if (activity == null || requestPermissions(activity))
      try {
        if (Looper.myLooper() == null)
          Looper.prepare();

        mCameraManager.openCamera(cameraID, openCameraStateCallback, null);
        Looper.loop();
      } catch (final Exception e) {
        UserInterface.showExceptionToNotification(e.toString(),
                "openCamera2AndCapture()");
      }
  }

  static void captureStillImage() {
    if (mCameraDevice == null)
      UserInterface.showExceptionToNotification(
              "captureStillImage() found null camera device!",
              "captureStillImage()");
    try {
      mCameraDevice.createCaptureSession(Collections.singletonList(
              getCaptureImageReader().getSurface()),
              getCaptureStillImageStateCallback(), null);
    } catch (final CameraAccessException e) {
      UserInterface.showExceptionToNotification(e.toString(),
              "captureStillImage()");
    }
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

  private static CameraCaptureSession.StateCallback getCaptureStillImageStateCallback() {
    if (mStateCallback == null)
      mStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureStillImageStateCallback#onConfigured: ");
          try {
            mCameraCaptureSession = cameraCaptureSession;
            cameraCaptureSession.capture(getStillImageCaptureRequest(),
                    getCaptureCallback(), null);

          } catch (final Exception e) {
            UserInterface.showExceptionToNotification(e.toString(),
                    "mCaptureStillImageStateCallback#onConfigured()");
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          UserInterface.showExceptionToNotification(
                  "CameraCaptureSession#StateCallBack() returns " +
                          "onConfigureFailed()",
                  "mCaptureStillImageStateCallback#onConfigureFailed()")
          ;
        }
      };
    return mStateCallback;
  }

  static CaptureRequest getStillImageCaptureRequest()
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
    final Activity activity = MainActivity.weakMainActivity.get();
    if (activity != null)
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

  static CameraCaptureSession.CaptureCallback getCaptureCallback() {
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
//            UserInterface.showExceptionToDialog(activity, e);
//          }
          closeCamera2();
        }
      };
    return mCaptureCallback;
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
