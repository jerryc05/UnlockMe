package jerryc05.unlockme;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public final class InstrumentedTest {

  private static Context context = ApplicationProvider.getApplicationContext();

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testURI() {
    Log.e("INTERNAL_CONTENT_URI", MediaStore.Images.Media.
            INTERNAL_CONTENT_URI.getPath());
    Log.e("EXTERNAL_CONTENT_URI", MediaStore.Images.Media.
            EXTERNAL_CONTENT_URI.getPath());
    Log.e("VOLUME_INTERNAL", MediaStore.Images.Media.
            getContentUri(MediaStore.VOLUME_INTERNAL).getPath());
    Log.e("VOLUME_EXTERNAL", MediaStore.Images.Media.
            getContentUri(MediaStore.VOLUME_EXTERNAL).getPath());
    Log.e("VOLUME_EXTERNAL_PRIMARY", MediaStore.Images.Media.
            getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY).getPath());

/*

E/INTERNAL_CONTENT_URI: /internal/images/media
E/EXTERNAL_CONTENT_URI: /external/images/media
E/VOLUME_INTERNAL: /internal/images/media
E/VOLUME_EXTERNAL: /external/images/media
E/VOLUME_EXTERNAL_PRIMARY: /external_primary/images/media

 */
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testDir() {
    for (File file : context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES))
      Log.e("getExternalFilesDirs", file.getAbsolutePath());
    Log.e("getExternalFilesDir", context.getExternalFilesDir(
            Environment.DIRECTORY_PICTURES).getAbsolutePath());
    for (File file : context.getExternalMediaDirs())
      Log.e("getExternalMediaDirs", file.getAbsolutePath());

/*

E/getExternalFilesDirs: /storage/emulated/0/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalFilesDirs: /storage/0670-0B16/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalFilesDir: /storage/emulated/0/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalMediaDirs: /storage/emulated/0/Android/media/jerryc05.unlockme
E/getExternalMediaDirs: /storage/0670-0B16/Android/media/jerryc05.unlockme

 */
  }
}
