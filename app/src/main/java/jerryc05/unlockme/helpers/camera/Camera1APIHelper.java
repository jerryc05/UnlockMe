package jerryc05.unlockme.helpers.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.util.Log;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.helpers.UserInterface;

final class Camera1APIHelper extends CameraBaseAPIClass {

  private static String          TAG          =
          Camera1APIHelper.class.getSimpleName();
  private static int             predefinedFacing;
  private static int             cameraID;
  private static Camera          mCamera;
  private static PictureCallback mJpegPictureCallback;
  @SuppressWarnings("WeakerAccess")
  static         int             captureCount = 0;
  @SuppressWarnings("WeakerAccess")
  static         SurfaceTexture  surfaceTexture;

  static void getImage(final int facing, final Context context) {
    predefinedFacing = facing;
    setupCamera1();
    openCamera1(context);
    captureCamera1(context);
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

  private static void openCamera1(final Context context) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "openCamera1: ");

    try {
      mCamera = Camera.open(cameraID);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        mCamera.enableShutterSound(false);
    } catch (Exception e) {
      UserInterface.showExceptionToNotification(
              e.toString(), "openCamera1()", context);
    }
  }

  @SuppressWarnings("WeakerAccess")
  static void captureCamera1(final Context context) {
    try {
      if (surfaceTexture == null)
        surfaceTexture =
                new SurfaceTexture(-1);
      mCamera.setPreviewTexture(surfaceTexture);
      mCamera.startPreview();
      mCamera.takePicture(null, null,
              getJpegPictureCallback(context));
      captureCount++;

    } catch (Exception e) {
      closeCamera1(mCamera);
      UserInterface.showExceptionToNotification(
              e.toString(), "captureCamera1()", context);
    }
  }

  @SuppressWarnings("WeakerAccess")
  static PictureCallback getJpegPictureCallback(final Context context) {
    if (mJpegPictureCallback == null)
      mJpegPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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

        }
      };
    return mJpegPictureCallback;
  }

  @SuppressWarnings("WeakerAccess")
  static void closeCamera1(Camera camera) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "closeCamera1: ");

    camera.stopPreview();
    camera.release();
    mCamera = null;
  }
}
