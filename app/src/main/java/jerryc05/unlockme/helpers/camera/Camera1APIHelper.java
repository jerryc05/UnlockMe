package jerryc05.unlockme.helpers.camera;

import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceView;

import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.helpers.UserInterface;

abstract class Camera1APIHelper extends CameraBaseAPIClass {

  private static int                      predefinedFacing;
  private static int                      cameraID;
  private static Camera                   mCamera;
  private static Camera.PictureCallback   mJpegPictureCallback;
  private static Camera.AutoFocusCallback mAutofocusCallback;

  static void getImage( final int facing) {
    predefinedFacing = facing;
    setupCamera1();
    openCamera1();
    captureCamera1();
  }

  private static void setupCamera1() {
    final int         numberOfCameras = Camera.getNumberOfCameras();
    Camera.CameraInfo cameraInfo      = new Camera.CameraInfo();

    for (int i = 0; i < numberOfCameras; i++) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == predefinedFacing) {
        cameraID = i;
        break;
      }
    }
  }

  private static void openCamera1() {
    try {
      mCamera = Camera.open(cameraID);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        mCamera.enableShutterSound(false);
    } catch (Exception e) {
      UserInterface.showExceptionToNotification(
              e.toString(), "openCamera1()");
    }
  }

  private static void captureCamera1() {
    try {
      mCamera.setPreviewDisplay(new SurfaceView(
              MainActivity.applicationContext).getHolder());
      mCamera.setPreviewCallback(new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {

        }
      });
      mCamera.startPreview();
      mCamera.autoFocus(getAutofocusCallback());
    } catch (Exception e) {
      UserInterface.showExceptionToNotification(
              e.toString(), "captureCamera1()");
    }
  }

  private static Camera.AutoFocusCallback getAutofocusCallback() {
    if (mAutofocusCallback == null)
      mAutofocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
          if (b)
            camera.takePicture(null, null, getJpegPictureCallback());
          else
            UserInterface.showExceptionToNotification(
                    "Autofocus failed!", "getAutofocusCallback()");
        }
      };
    return mAutofocusCallback;
  }

  @SuppressWarnings("WeakerAccess")
  static Camera.PictureCallback getJpegPictureCallback() {
    if (mJpegPictureCallback == null)
      mJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
          camera.stopPreview();
          camera.release();
          saveImageToDisk(data);
        }
      };
    return mJpegPictureCallback;
  }
}
