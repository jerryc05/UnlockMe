package jerryc05.unlockme

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTest {
  @Test
  fun testURI() {
    Log.e("INTERNAL_CONTENT_URI", MediaStore.Images.Media.INTERNAL_CONTENT_URI.path!!)
    Log.e("EXTERNAL_CONTENT_URI", MediaStore.Images.Media.EXTERNAL_CONTENT_URI.path!!)
    Log.e(
      "VOLUME_INTERNAL",
      MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_INTERNAL).path!!
    )
    Log.e(
      "VOLUME_EXTERNAL",
      MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL).path!!
    )
    Log.e(
      "VOLUME_EXTERNAL_PRIMARY",
      MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY).path!!
    )

/*

E/INTERNAL_CONTENT_URI: /internal/images/media
E/EXTERNAL_CONTENT_URI: /external/images/media
E/VOLUME_INTERNAL: /internal/images/media
E/VOLUME_EXTERNAL: /external/images/media
E/VOLUME_EXTERNAL_PRIMARY: /external_primary/images/media

 */
  }

  @Test
  fun testDir() {
    for (file in context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES)) Log.e(
      "getExternalFilesDirs",
      file.absolutePath
    )
    Log.e(
      "getExternalFilesDir", context.getExternalFilesDir(
        Environment.DIRECTORY_PICTURES
      )!!.absolutePath
    )
    for (file in context.externalMediaDirs) Log.e("getExternalMediaDirs", file.absolutePath)

/*

E/getExternalFilesDirs: /storage/emulated/0/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalFilesDirs: /storage/0670-0B16/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalFilesDir: /storage/emulated/0/Android/data/jerryc05.unlockme/files/Pictures
E/getExternalMediaDirs: /storage/emulated/0/Android/media/jerryc05.unlockme
E/getExternalMediaDirs: /storage/0670-0B16/Android/media/jerryc05.unlockme

 */
  }

  companion object {
    private val context = ApplicationProvider.getApplicationContext<Context>()
  }
}