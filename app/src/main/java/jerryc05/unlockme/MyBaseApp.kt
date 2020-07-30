package jerryc05.unlockme

import android.app.Application
import jerryc05.unlockme.helpers.UserInterface
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MyBaseApp : Application() {

  companion object {
    lateinit var threadPoolExecutor: ThreadPoolExecutor
  }

  override fun onCreate() {
    super.onCreate()

    // Thread Pool Executor
    run {
      val rejectedExecutionHandler = RejectedExecutionHandler { runnable, _ ->
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
  }
}