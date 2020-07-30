package jerryc05.unlockme.activities

import android.app.Activity
import android.os.Bundle
import jerryc05.unlockme.helpers.UserInterface
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class MyBaseActivity : Activity() {

  companion object {
    lateinit var threadPoolExecutor: ThreadPoolExecutor
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val rejectedExecutionHandler =
      RejectedExecutionHandler { runnable, _ ->
        UserInterface.showExceptionToNotification(
          this, "ThreadPoolExecutor rejected: $runnable",
          "threadPoolExecutor#rejectedExecutionHandler()"
        )
      }
    val cores = Runtime.getRuntime().availableProcessors()
    threadPoolExecutor = ThreadPoolExecutor(
      cores, 2 * cores, 5, TimeUnit.SECONDS,
      LinkedBlockingQueue(1), rejectedExecutionHandler
    )
    threadPoolExecutor.allowCoreThreadTimeOut(true)
  }

  override fun onDestroy() {
    threadPoolExecutor.shutdownNow()
    super.onDestroy()
  }
}