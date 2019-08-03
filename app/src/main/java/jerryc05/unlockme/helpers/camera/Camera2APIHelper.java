package jerryc05.unlockme.helpers.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.UserInterface;

@SuppressWarnings("WeakerAccess")
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class Camera2APIHelper extends CameraBaseAPIClass {

  static final   String                             TAG          =
          "Camera2APIHelper";
  private static int                                predefinedFacing;
  static         String                             cameraID;
  static         CameraManager                      mCameraManager;
  static         CameraDevice                       mCameraDevice;
  private static CameraCharacteristics              mCameraCharacteristics;
  static         CameraCaptureSession               mCameraCaptureSession;
  static         CameraDevice.StateCallback         openCameraStateCallback;
  private static SparseIntArray                     orientationsMap;
  private static ImageReader                        mImageReader;
  private static CaptureCallback                    mCaptureCallback;
  private static OnImageAvailableListener           onImageAvailableListener;
  private static CameraCaptureSession.StateCallback mStateCallback;
  static         int                                captureCount = 0;

  public static void getImage(int facing, @NonNull final Context context) {
    predefinedFacing = facing;
    setupCamera2(context);
    openCamera2(context);
  }

  private static void setupCamera2(@NonNull final Context context) {
    getCameraIDAndCharacteristics(context);
  }

  private static void getCameraIDAndCharacteristics(@NonNull final Context context) {
    if (cameraID == null)
      try {
        for (String eachCameraID : getCameraManager(context).getCameraIdList()) {

          final CameraCharacteristics eachCameraCharacteristics =
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
        UserInterface.throwExceptionAsNotification(
                e.toString(), "getCameraIDAndCharacteristics", context);
      }
    if (cameraID == null)
      UserInterface.throwExceptionAsNotification(
              "getCameraIDAndCharacteristics() cannot find Front Camera!",
              "getCameraIDAndCharacteristics", context);
  }

  private static CameraManager getCameraManager(@NonNull final Context context) {
    if (mCameraManager == null)
      mCameraManager = (CameraManager)
              context.getSystemService(Context.CAMERA_SERVICE);
    assert mCameraManager != null;
    return mCameraManager;
  }

  private static CameraDevice.StateCallback getOpenCameraStateCallback(
          @NonNull final Context context) {
    if (openCameraStateCallback == null)
      openCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull final CameraDevice cameraDevice) {
          mCameraDevice = cameraDevice;
          captureStillImage(context);
        }

        @Override
        public void onDisconnected(@NonNull final CameraDevice cameraDevice) {
          cameraDevice.close();
          mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull final CameraDevice cameraDevice, int error) {
          onDisconnected(cameraDevice);
          UserInterface.throwExceptionAsNotification(
                  "openCameraStateCallback#onError() returns error code: "
                          + error + '!', "onError()", context);
        }
      };
    return openCameraStateCallback;
  }

  @SuppressLint("MissingPermission")
  private static void openCamera2(@NonNull final Context context) {
    if (requestPermissions(context))
      try {
        if (Looper.myLooper() == null)
          Looper.prepare();
        mCameraManager.openCamera(cameraID,
                getOpenCameraStateCallback(context), null);
        Looper.loop();
      } catch (final Exception e) {
        UserInterface.throwExceptionAsNotification(e.toString(),
                "openCamera2()", context);
      }
  }

  static void captureStillImage(@NonNull final Context context) {
    try {
      mCameraDevice.createCaptureSession(Collections.singletonList(
              getImageReader(context).getSurface()),
              getCaptureStillImageStateCallback(context), null);
    } catch (final Exception e) {
      UserInterface.throwExceptionAsNotification(e.toString(),
              "captureStillImage()", context);
    }
  }

  static ImageReader getImageReader(@NonNull final Context context) {
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
              getOnImageAvailableListener(context), null);
    }
    return mImageReader;
  }

  private static CameraCaptureSession.StateCallback getCaptureStillImageStateCallback(
          @NonNull final Context context) {
    if (mStateCallback == null)
      mStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(
                @NonNull final CameraCaptureSession cameraCaptureSession) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureStillImageStateCallback#onConfigured: ");

          mCameraCaptureSession = cameraCaptureSession;
          captureCamera2(context);
        }

        @Override
        public void onConfigureFailed(
                @NonNull final CameraCaptureSession session) {
          UserInterface.throwExceptionAsNotification(
                  "$mStateCallback returns onConfigureFailed()",
                  "onConfigureFailed()",
                  context);
        }
      };
    return mStateCallback;
  }

  static void captureCamera2(@NonNull final Context context) {
    try {
      mCameraCaptureSession.capture(getStillImageCaptureRequest(context),
              getCaptureCallback(context), null);
      captureCount++;

    } catch (final Exception e) {
      UserInterface.throwExceptionAsNotification(e.toString(),
              "mCaptureStillImageStateCallback#onConfigured()",
              context);
    }
  }

  static CaptureRequest getStillImageCaptureRequest(@NonNull final Context context)
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
    mCaptureRequestBuilder.addTarget(
            getImageReader(context).getSurface());

    if (context instanceof Activity)
      mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
              (predefinedFacing == CameraCharacteristics.LENS_FACING_FRONT
                      ? getFrontOrientationsMap()
                      : getBackOrientationsMap()).get(
                      ((Activity) context).getWindowManager()
                              .getDefaultDisplay().getRotation()));
    else
      mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);

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

  private static OnImageAvailableListener getOnImageAvailableListener(
          @NonNull final Context context) {
    if (onImageAvailableListener == null)
      onImageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(@NonNull final ImageReader imageReader) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mOnImageAvailableListener#onImageAvailable(): ");

          final Image      image  = imageReader.acquireLatestImage();
          final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
          final byte[]     bytes  = new byte[buffer.capacity()];
          buffer.get(bytes);
          saveImageToDisk(bytes, context);
          image.close();
        }
      };
    return onImageAvailableListener;
  }

  static CaptureCallback getCaptureCallback(@NonNull final Context context) {
    if (mCaptureCallback == null)
      mCaptureCallback = new CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull final CameraCaptureSession session,
                                       @NonNull final CaptureRequest request,
                                       @NonNull final TotalCaptureResult result) {
          if (BuildConfig.DEBUG)
            Log.d(TAG, "mCaptureCallback#onCaptureCompleted: ");

          if (captureCount < imageCount)
            captureCamera2(context);
          else {
            captureCount = 0;
            closeCamera2();
          }
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

    final Looper myLooper = Looper.myLooper();
    if (myLooper != null &&
            myLooper != Looper.getMainLooper())
      myLooper.quit();
  }

  public static void trimMemory() {
    cameraID = null;
    mCameraManager = null;
    mCameraDevice = null;
    mCameraCharacteristics = null;
    mCameraCaptureSession = null;
    openCameraStateCallback = null;
    orientationsMap = null;
    mImageReader = null;
    mCaptureCallback = null;
    onImageAvailableListener = null;
    mStateCallback = null;
  }
}
