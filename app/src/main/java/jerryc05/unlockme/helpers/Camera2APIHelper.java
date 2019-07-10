package jerryc05.unlockme.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import jerryc05.unlockme.BuildConfig;
import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;

public final class Camera2APIHelper {

  private static final String                     TAG =
          Camera2APIHelper.class.getSimpleName();
  protected static     String                     cameraID;
  private static       CameraCharacteristics      mCameraCharacteristics;
  protected static     CameraManager              mCameraManager;
  private static       CameraCaptureSession       mCameraCaptureSession;
  private static       CameraDevice.StateCallback mStateCallback;
  protected static     CameraDevice               mCameraDevice;
  private static       SparseIntArray             ORIENTATIONS;


  public static void automaticTakePhoto(MainActivity activity) {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "automaticTakePhoto: ");

    if (requestCameraPermission(activity)) {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "automaticTakePhoto: Permission granted!");


//      if (cameraID == null || mStateCallback == null)
//        setupCamera2(activity);
//      openCamera2(activity);
    }
  }
  private static boolean requestCameraPermission(MainActivity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            activity.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED)
      return true;

    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Permission Required")
                  .setMessage("We need CAMERA permission to work properly.")
                  .setCancelable(false)
                  .setPositiveButton("OK",
                          new DialogInterface.OnClickListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onClick(DialogInterface dialogInterface,
                                                int i) {
                              activity.requestPermissions(
                                      new String[]{Manifest.permission.CAMERA},
                                      MainActivity.REQUEST_CODE_CAMERA);
                            }
                          })
                  .show();
        }
      });
    else
      activity.requestPermissions(new String[]{Manifest.permission.CAMERA},
              MainActivity.REQUEST_CODE_CAMERA);

    return false;
  }

/*
  private static void setupCamera2(MainActivity activity) {
    if (mCameraManager == null)
      mCameraManager =
              (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      assert mCameraManager != null;
      for (String eachCameraID : mCameraManager.getCameraIdList()) {

        CameraCharacteristics characteristics =
                mCameraManager.getCameraCharacteristics(eachCameraID);

        final Integer FACING =
                characteristics.get(CameraCharacteristics.LENS_FACING);
        if (FACING == null ||
                FACING != CameraCharacteristics.LENS_FACING_FRONT)
          continue;

        cameraID = eachCameraID;
        mCameraCharacteristics = characteristics;
        break;
      }
      if (cameraID == null)
        throw new UnsupportedOperationException("Cannot find Front Camera!");

      if (mStateCallback == null)
        mStateCallback = new CameraDevice.StateCallback() {

          @Override
          public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            try {
              captureCamera(activity);
            } catch (CameraAccessException e) {
              activity.alertExceptionToUI(e);
            }
          }

          @Override
          public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
          }

          @Override
          public void onClosed(CameraDevice camera) {
            capturingListener.onDoneCapturingAllPhotos(picturesTaken);
          }

          @Override
          public void onError(CameraDevice cameraDevice, int error) {
            onDisconnected(cameraDevice);
            activity.alertExceptionToUI(new UnsupportedOperationException(
                    "CameraDevice#StateCallback() returns error, code: "
                            + error));
          }
        };

    } catch (Exception e) {
      activity.alertExceptionToUI(e);
    }
  }

  private static void openCamera2(MainActivity activity) {
    if (requestCameraPermission(activity))
      activity.runOnUiThread(new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
          try {
            mCameraManager.openCamera(cameraID, mStateCallback, null);
          } catch (CameraAccessException e) {
            activity.alertExceptionToUI(e);
          }
        }
      });
  }

  protected static void captureCamera(MainActivity activity)
          throws CameraAccessException {

    if (null == mCameraDevice) {
      Log.e(TAG, "mCameraDevice is null");
      return;
    }
    Size[] jpegSizes = null;
    StreamConfigurationMap streamConfigurationMap =
            mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if (streamConfigurationMap != null) {
      jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
    }
    final boolean       jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
    int                 width             = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
    int                 height            = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
    final ImageReader   imageReader       = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
    final List<Surface> outputSurfaces    = new ArrayList<>();
    outputSurfaces.add(imageReader.getSurface());
    final CaptureRequest.Builder captureBuilder =
            mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

    captureBuilder.addTarget(imageReader.getSurface());
    captureBuilder.set(
            CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO
    );
    if (ORIENTATIONS == null) {
      ORIENTATIONS = new SparseIntArray(4);
      ORIENTATIONS.append(Surface.ROTATION_0, 90);
      ORIENTATIONS.append(Surface.ROTATION_90, 0);
      ORIENTATIONS.append(Surface.ROTATION_180, 270);
      ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    captureBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            ORIENTATIONS.get(activity.getWindowManager().getDefaultDisplay().getRotation())
    );
    imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
    mCameraDevice.createCaptureSession(outputSurfaces,
            new CameraCaptureSession.StateCallback() {
              @Override
              public void onConfigured(CameraCaptureSession session) {
                try {
                  session.capture(captureBuilder.build(), captureListener, null);
                } catch (final CameraAccessException e) {
                  activity.alertExceptionToUI(e);
                }
              }

              @Override
              public void onConfigureFailed(CameraCaptureSession session) {
                activity.alertExceptionToUI(new UnsupportedOperationException(
                        "CameraCaptureSession#StateCallBack() returns onConfigureFailed()"
                ));
              }
            }, null);
  }

  private static final ImageReader.OnImageAvailableListener onImageAvailableListener =
          (ImageReader imReader) -> {
            final Image      image  = imReader.acquireLatestImage();
            final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            final byte[]     bytes  = new byte[buffer.capacity()];
            buffer.get(bytes);
            saveImageToDisk(bytes);
            image.close();
          };


  private static final CameraCaptureSession.CaptureCallback captureListener =
          new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                           TotalCaptureResult result) {
              super.onCaptureCompleted(session, request, result);
              if (picturesTaken.lastEntry() != null) {
                capturingListener.onCaptureDone(picturesTaken.lastEntry().getKey(), picturesTaken.lastEntry().getValue());
                Log.i(TAG, "done taking picture from camera " + mCameraDevice.getId());
              }
              closeCamera();
            }
          };


  private static void saveImageToDisk(final byte[] bytes) {
    final String cameraId = mCameraDevice == null ? UUID.randomUUID().toString() : this.mCameraDevice.getId();
    final File   file     = new File(Environment.getExternalStorageDirectory() + "/" + cameraId + "_pic.jpg");
    try (final OutputStream output = new FileOutputStream(file)) {
      output.write(bytes);
      this.picturesTaken.put(file.getPath(), bytes);
    } catch (final IOException e) {
      Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
    }
  }


  private static void closeCamera() {
    Log.d(TAG, "closing camera " + mCameraDevice.getId());
    if (null != mCameraDevice && !cameraClosed) {
      mCameraDevice.close();
      mCameraDevice = null;
    }
    if (null != imageReader) {
      imageReader.close();
      imageReader = null;
    }
  }


  protected static TreeMap<String, byte[]> picturesTaken = new TreeMap<>();


  protected static PictureCapturingListener capturingListener =
*/
}
