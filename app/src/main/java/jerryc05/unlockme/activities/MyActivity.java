package jerryc05.unlockme.activities;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jerryc05.unlockme.helpers.UserInterface;

abstract class MyActivity extends Activity {

  private static final String             TAG = "MyActivity";
  static               ThreadPoolExecutor threadPoolExecutor;

  static ThreadPoolExecutor getThreadPoolExecutor(
          @NonNull final Context context) {
    if (threadPoolExecutor == null) {
      final RejectedExecutionHandler rejectedExecutionHandler
              = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable runnable,
                                      ThreadPoolExecutor threadPoolExecutor) {
          UserInterface.showExceptionToNotification(
                  "ThreadPoolExecutor：\n>>> "
                          + threadPoolExecutor.toString()
                          + "\non " + TAG + " rejected:\n >>> "
                          + runnable.toString(),
                  "threadPoolExecutor#rejectedExecution()",
                  context);
        }
      };
      final int processorCount = Runtime.getRuntime().availableProcessors();
      threadPoolExecutor = new ThreadPoolExecutor(processorCount,
              2 * processorCount, 5, TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(1), rejectedExecutionHandler);
      threadPoolExecutor.allowCoreThreadTimeOut(true);
    }
    return threadPoolExecutor;
  }
}
