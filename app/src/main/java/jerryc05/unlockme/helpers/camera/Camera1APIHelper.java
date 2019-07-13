package jerryc05.unlockme.helpers.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;

import jerryc05.unlockme.helpers.UserInterface;

abstract class Camera1APIHelper extends CameraBaseAPIClass {

  private static int                    predefinedFacing;
  private static int                    cameraID;
  private static Camera                 mCamera;
  private static Camera.PictureCallback mJpegPictureCallback;

  static void getImage(final int facing) {
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

//      final Camera.Parameters mCameraParameters = mCamera.getParameters();
//      final List<Camera.Size> sizes             = mCameraParameters.getSupportedPictureSizes();
//      final Camera.Size mSize = Collections.max(sizes, new Comparator<Camera.Size>() {
//        @Override
//        public int compare(Camera.Size prev, Camera.Size next) {
//          final int width = prev.width - next.width;
//          return width != 0 ? width
//                  : prev.height - next.height;
//        }
//      });
//      mCameraParameters.setPictureSize(mSize.width, mSize.height);
//      mCamera.setParameters(mCameraParameters);
    } catch (Exception e) {
      UserInterface.showExceptionToNotification(
              e.toString(), "openCamera1()");
    }
  }

  private static void captureCamera1() {
    try {
      mCamera.setPreviewTexture(new SurfaceTexture(-1));
      mCamera.startPreview();
      mCamera.takePicture(null, null, getJpegPictureCallback());

    } catch (Exception e) {
      closeCamera1(mCamera);
      UserInterface.showExceptionToNotification(
              e.toString(), "captureCamera1()");
    }
  }

  @SuppressWarnings("WeakerAccess")
  static Camera.PictureCallback getJpegPictureCallback() {
    if (mJpegPictureCallback == null)
      mJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
          closeCamera1(camera);
          saveImageToDisk(data);
        }
      };
    return mJpegPictureCallback;
  }

  @SuppressWarnings("WeakerAccess")
  static void closeCamera1(Camera camera) {
    camera.stopPreview();
    camera.release();
    mCamera = null;
  }
}
