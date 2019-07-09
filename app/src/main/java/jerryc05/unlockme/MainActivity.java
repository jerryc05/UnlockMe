package jerryc05.unlockme;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import jerryc05.unlockme.receivers.MyDAReceiver;

@SuppressWarnings("NullableProblems")
public class MainActivity extends Activity {

  private final static String
          TAG                       = MainActivity.class.getName();
  private final static int
          REQUEST_CODE_CAMERA       = 0,
          REQUEST_CODE_DEVICE_ADMIN = 1;

  protected ReentrantLock         requestDeviceAdminLock;
  protected DevicePolicyManager   mDevicePolicyManager;
  private   ComponentName         mComponentName;
  private   String                cameraID;
  private   CameraCharacteristics mCameraCharacteristics;
  private   CameraManager         mCameraManager;
  private   CameraCaptureSession  mCameraCaptureSession;
  private   StateCallback         mStateCallback;
  protected CameraDevice          mCameraDevice;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

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

    if (requestRuntimePermissions()) {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "takePhoto: Permission acquired!");

      if (cameraID == null || mStateCallback == null)
        setupCamera2();
      openCamera2();
    }
  }

  private boolean requestRuntimePermissions() {
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
            //创建CameraPreviewSession
//                createCameraPreviewSession();
//                    try {
//                      mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//                      mCaptureRequestBuilder.addTarget(previewSurface);
//                      cameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
//                        @Override
//                        public void onConfigured(CameraCaptureSession session) {
//                          try {
//                            mCaptureRequest = mCaptureRequestBuilder.build();
//                            mCameraCaptureSession = session;
//                            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
//                          } catch (CameraAccessException e) {
//                            e.printStackTrace();
//                          }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(CameraCaptureSession session) {
//
//                        }
//                      }, mCameraHandler);
//                    } catch (CameraAccessException e) {
//                      e.printStackTrace();
//                    }
          }

          @Override
          public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
      try {
        mCameraManager.openCamera(cameraID, mStateCallback, null);
      } catch (CameraAccessException e) {
        AlertExceptionToUI(e);
      }
    } else
      requestRuntimePermissions();
  }

  private void AlertExceptionToUI(Exception e) {
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
                .setTitle("Fatal Error")
                .setMessage(e.toString())
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }
}
