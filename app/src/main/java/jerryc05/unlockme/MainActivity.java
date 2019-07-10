package jerryc05.unlockme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.helpers.URLConnectionBuilder;
import jerryc05.unlockme.receivers.MyDAReceiver;

@SuppressWarnings("NullableProblems")
public class MainActivity extends Activity {

  private final static String
          TAG                       = MainActivity.class.getName();
  private final static int
          REQUEST_CODE_CAMERA       = 0,
          REQUEST_CODE_DEVICE_ADMIN = 1;

  protected            ReentrantLock         requestDeviceAdminLock;
  protected            DevicePolicyManager   mDevicePolicyManager;
  private              ComponentName         mComponentName;
  protected            String                cameraID;
  private              CameraCharacteristics mCameraCharacteristics;
  protected            CameraManager         mCameraManager;
  private              CameraCaptureSession  mCameraCaptureSession;
  protected            StateCallback         mStateCallback;
  protected            CameraDevice          mCameraDevice;
  private static final SparseIntArray        ORIENTATIONS = 
          new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    new Thread(new Runnable() {
      @Override
      public void run() {
        checkUpdate();
        findViewById(R.id.activity_main_button_takePhoto)
                .setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                    new Thread(new Runnable() {
                      @Override
                      public void run() {
                        takePhoto();
                      }
                    }).start();
                  }
                });
      }
    }).start();
  }

  @Override
  protected void onResume() {
    super.onResume();

    new Thread(new Runnable() {
      @Override
      public void run() {
        if (requestDeviceAdminLock == null)
          requestDeviceAdminLock = new ReentrantLock();
        requestDeviceAdminLock.lock();
        requestDeviceAdmin();
      }
    }).start();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_DEVICE_ADMIN &&
            !mDevicePolicyManager.isAdminActive(mComponentName)) {
      if (requestDeviceAdminLock == null)
        requestDeviceAdminLock = new ReentrantLock();
      requestDeviceAdminLock.lock();
      AlertExceptionToUI(
              new UnsupportedOperationException(
                      "Device Admin permission not acquired!"),
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  System.exit(1);
                }
              });
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions,
                                         int[] grantResults) {
    if (requestCode == REQUEST_CODE_CAMERA) {
      final boolean granted = grantResults.length > 0 &&
              grantResults[0] == PackageManager.PERMISSION_GRANTED;
      final String granted_str = granted
              ? "Camera Permission Granted √"
              : "Camera Permission Denied ×";

      if (BuildConfig.DEBUG)
        Log.d(TAG, "onRequestPermissionsResult: " + granted);

      Toast.makeText(this,
              granted_str, Toast.LENGTH_SHORT)
              .show();
      if (granted) takePhoto();
    }
  }

  protected void checkUpdate() {
    final String
            keyword = "/jerryc05/UnlockMe/tree",
            URL = "https://www.github.com/jerryc05/UnlockMe/releases";
    try (URLConnectionBuilder connectionBuilder = URLConnectionBuilder
            .get(URL)
            .setConnectTimeout(1000)
            .setReadTimeout(1000)
            .connect()) {
      String result = connectionBuilder.getResult();
      result = result.substring(result.indexOf(keyword) +
              keyword.length() + 2);
      final String latest = result.substring(0, result.indexOf('"'));

      if (!latest.equals(BuildConfig.VERSION_NAME))
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("New Version Available")
                    .setMessage("Do you want to upgrade from\n" +
                            BuildConfig.VERSION_NAME + "  to  " + latest + '?')
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialogInterface,
                                                  int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(URL + "/tag/v" + latest)));
                              }
                            })
                    .setNegativeButton("NO", null)
                    .show();
          }
        });

    } catch (Exception e) {
      AlertExceptionToUI(e);
    }
  }

  void requestDeviceAdmin() {
    if (requestDeviceAdminLock != null) {
      requestDeviceAdminLock.unlock();
      requestDeviceAdminLock = null;
    }
    if (mDevicePolicyManager == null)
      mDevicePolicyManager = (DevicePolicyManager) Objects.requireNonNull(
              getSystemService(Context.DEVICE_POLICY_SERVICE));
    if (mComponentName == null)
      mComponentName =
              new ComponentName(getApplicationContext(), MyDAReceiver.class);

    if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
      final Intent intentDeviceAdmin = new Intent(
              DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
      intentDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
              mComponentName);
      intentDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
              getResources().getString(R.string.device_admin_explanation));

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          startActivityForResult(intentDeviceAdmin, REQUEST_CODE_DEVICE_ADMIN);
        }
      });
    } else if (BuildConfig.DEBUG)
      Log.d(TAG, "requestDeviceAdmin: DA is activated!");
  }

  void takePhoto() {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "takePhoto: ");

    if (requestCameraPermission()) {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "takePhoto: Permission acquired!");

      if (cameraID == null || mStateCallback == null)
        setupCamera2();
      openCamera2();
    }
  }

  private boolean requestCameraPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED)
      return true;

    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(MainActivity.this)
                  .setTitle("Permission Required")
                  .setMessage(getResources().getString(
                          R.string.permission_camera_explanation))
                  .setCancelable(false)
                  .setPositiveButton("OK",
                          new DialogInterface.OnClickListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onClick(DialogInterface dialogInterface,
                                                int i) {
                              requestPermissions(
                                      new String[]{Manifest.permission.CAMERA},
                                      REQUEST_CODE_CAMERA);
                            }
                          })
                  .show();
        }
      });
    else
      requestPermissions(new String[]{Manifest.permission.CAMERA},
              REQUEST_CODE_CAMERA);

    return false;
  }

  private void setupCamera2() {
    if (mCameraManager == null)
      mCameraManager =
              (CameraManager) getSystemService(Context.CAMERA_SERVICE);
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
              captureCamera();
            } catch (CameraAccessException e) {
              AlertExceptionToUI(e);
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
            cameraDevice.close();
            mCameraDevice = null;
          }
        };

    } catch (Exception e) {
      AlertExceptionToUI(e);
    }
  }

  private void openCamera2() {
    if (requestCameraPermission())
      runOnUiThread(new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
          try {
            mCameraManager.openCamera(cameraID, mStateCallback, null);
          } catch (CameraAccessException e) {
            AlertExceptionToUI(e);
          }
        }
      });
  }

  protected void captureCamera() throws CameraAccessException {

    if (null == mCameraDevice) {
      Log.e(TAG, "mCameraDevice is null");
      return;
    }
    Size[]                      jpegSizes              = null;
    StreamConfigurationMap      streamConfigurationMap =
            mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if (streamConfigurationMap != null) {
      jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
    }
    final boolean       jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
    int                 width             = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
    int                 height            = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
    final ImageReader   imageReader            = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
    final List<Surface> outputSurfaces    = new ArrayList<>();
    outputSurfaces.add(imageReader.getSurface());
    final CaptureRequest.Builder captureBuilder = 
            mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
    
    captureBuilder.addTarget(imageReader.getSurface());
    captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
     ORIENTATIONS.get(getWindowManager().getDefaultDisplay().getRotation()));
    imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
    mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
              @Override
              public void onConfigured( CameraCaptureSession session) {
                try {
                  session.capture(captureBuilder.build(), captureListener, null);
                } catch (final CameraAccessException e) {
                  AlertExceptionToUI(e);
                }
              }

              @Override
              public void onConfigureFailed( CameraCaptureSession session) {
              }
            }
            , null);
  }

  private final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader imReader) -> {
    final Image      image  = imReader.acquireLatestImage();
    final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    final byte[]     bytes  = new byte[buffer.capacity()];
    buffer.get(bytes);
    saveImageToDisk(bytes);
    image.close();
  };



  protected final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureCompleted( CameraCaptureSession session,  CaptureRequest request,
                                    TotalCaptureResult result) {
      super.onCaptureCompleted(session, request, result);
      if (picturesTaken.lastEntry() != null) {
        capturingListener.onCaptureDone(picturesTaken.lastEntry().getKey(), picturesTaken.lastEntry().getValue());
        Log.i(TAG, "done taking picture from camera " + mCameraDevice.getId());
      }
      closeCamera();
    }
  };





  private void saveImageToDisk(final byte[] bytes) {
    final String cameraId = mCameraDevice == null ? UUID.randomUUID().toString() : this.mCameraDevice.getId();
    final File   file     = new File(Environment.getExternalStorageDirectory() + "/" + cameraId + "_pic.jpg");
    try (final OutputStream output = new FileOutputStream(file)) {
      output.write(bytes);
      this.picturesTaken.put(file.getPath(), bytes);
    } catch (final IOException e) {
      Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
    }
  }


  protected void closeCamera() {
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


  protected TreeMap<String, byte[]> picturesTaken=new TreeMap<>();


protected PictureCapturingListener capturingListener =











  void AlertExceptionToUI(Exception e) {
    AlertExceptionToUI(e, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        throw new UnsupportedOperationException(e);
      }
    });
  }

  private void AlertExceptionToUI(Exception e,
                                  DialogInterface.OnClickListener onClickListener) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Crash Report")
                .setMessage(e.toString())
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }
}
