package jerryc05.unlockme.helpers.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import jerryc05.unlockme.BuildConfig;

import static jerryc05.unlockme.helpers.UserInterface.throwExceptionAsNotification;

final class Camera1APIHelper extends CameraBaseAPIClass {

  private static final String          TAG          = "Camera1APIHelper";
  private static       int             predefinedFacing;
  private static       int             cameraID;
  private static       Camera          mCamera;
  private static       PictureCallback mJpegPictureCallback;
  private static       int             captureCount = 0;
  private static       SurfaceTexture  surfaceTexture;

  static void getImage(int facing, @NonNull final Context context) {
    predefinedFacing = facing;
    setupCamera1();
    openCamera1(context);
    captureCamera1(context);

    if (Looper.myLooper() == null)
      Looper.prepare();
    Looper.loop();
  }

  private static void setupCamera1() {
    final int        numberOfCameras = Camera.getNumberOfCameras();
    final CameraInfo cameraInfo      = new CameraInfo();

    for (int i = 0; i < numberOfCameras; i++) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == predefinedFacing) {
        cameraID = i;
        break;
      }
    }
  }

  private static void openCamera1(@NonNull final Context context) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "openCamera1: ");

    try {
      mCamera = Camera.open(cameraID);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        mCamera.enableShutterSound(false);
    } catch (final Exception e) {
      throwExceptionAsNotification(context,
              e.toString(), "openCamera1()");
    }
  }

  private static void captureCamera1(@NonNull final Context context) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "captureCamera1: ");

    try {
      mCamera.setPreviewTexture(getSurfaceTexture());
      mCamera.startPreview();
      if (BuildConfig.DEBUG)
        Log.d(TAG, "captureCamera1: Preview started!");

      mCamera.takePicture(null, null,
              getJpegPictureCallback(context));
      if (BuildConfig.DEBUG)
        Log.d(TAG, "captureCamera1: Take picture called!!");

      captureCount++;

    } catch (final Exception e) {
      closeCamera1(mCamera);
      throwExceptionAsNotification(context, e.toString(),
              "captureCamera1()");
    }
  }

  private static SurfaceTexture getSurfaceTexture() {
    if (surfaceTexture == null)
      surfaceTexture = new SurfaceTexture(-1);
    return surfaceTexture;
  }

  private static PictureCallback getJpegPictureCallback(
          @NonNull final Context context) {
    if (mJpegPictureCallback == null)
      mJpegPictureCallback = (data, camera) -> {
        if (BuildConfig.DEBUG)
          Log.d(TAG, "onPictureTaken: ");

        saveImageToDisk(data, context);

        if (captureCount < imageCount)
          captureCamera1(context);
        else {
          captureCount = 0;
          if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
          }
          closeCamera1(camera);
        }
      };
    return mJpegPictureCallback;
  }

  private static void closeCamera1(@NonNull final Camera camera) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "closeCamera1: ");

    camera.stopPreview();
    camera.release();
    mCamera = null;

    final Looper myLooper = Looper.myLooper();
    if (myLooper != null &&
            myLooper != Looper.getMainLooper())
      myLooper.quit();
  }

  public static void trimMemory() {
    mCamera = null;
    mJpegPictureCallback = null;
    surfaceTexture = null;
  }
}
