package jerryc05.unlockme.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Build;

import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * A collection class for commonly used User Interface methods.
 *
 * @author \
 * \           d88b d88888b d8888b. d8888b. db    db  .o88b.  .d88b.    ooooo
 * \           `8P' 88'     88  `8D 88  `8D `8b  d8' d8P  Y8 .8P  88.  8P~~~~
 * \            88  88ooooo 88oobY' 88oobY'  `8bd8'  8P      88  d'88 dP
 * \            88  88~~~~~ 88`8b   88`8b      88    8b      88 d' 88 V8888b.
 * \        db. 88  88.     88 `88. 88 `88.    88    Y8b  d8 `88  d8'     `8D
 * \        Y8888P  Y88888P 88   YD 88   YD    YP     `Y88P'  `Y88P'  88oobY'
 * @see android.app.AlertDialog
 * @see android.app.Notification
 */
public abstract class UserInterface {

  private static NotificationManager notificationManager;

  @SuppressWarnings("unused")
  public static void showExceptionToDialog(final Context context, final Exception e) {
    if (context == null)
      return;

    showExceptionToDialog(context, e, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        throw new UnsupportedOperationException(e);
      }
    });
  }

  @SuppressWarnings("WeakerAccess")
  public static void showExceptionToDialog(final Context context, final Exception e,
                                           final DialogInterface.OnClickListener onClickListener) {
    if (!(context instanceof Activity))
      return;

    ((Activity) context).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(context)
                .setTitle("Crash Report")
                .setMessage(e.toString())
                .setIcon(R.drawable.ic_round_error_24px)
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }

  public static void showExceptionToNotification(final String contentText,
                                                 final String subText) {
    showExceptionToNotificationNoRethrow(contentText, subText);
    throw new UnsupportedOperationException(contentText);
  }

  public static void showExceptionToNotificationNoRethrow(final String contentText,
                                                          final String subText) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle("Crash Report")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground)
            .setSubText(subText)
            .setStyle(new Notification.BigTextStyle()
                    .bigText(contentText));

    getNotificationManager().notify(-1, setNotificationChannel(builder,
            getNotificationManager(),
            "Crash Report",
            "Crash report notification channel for UnlockMe",
            true).build());
  }

  @SuppressWarnings("unused")
  public static void notifyPictureToUI(final String contentText,
                                       final byte[] bytes) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle("Picture Taken")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground)
            .setStyle(new Notification.BigPictureStyle()
                    .bigPicture(BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length)));

    getNotificationManager().notify(-1, setNotificationChannel(builder,
            getNotificationManager(),
            "Image Captured Report",
            "Image captured report notification channel for UnlockMe",
            false).build());
  }

  @SuppressWarnings("unused")
  public static void notifyToUI(final String title,
                                final String contentText) {
    notifyToUI(title, contentText, MainActivity.applicationContext);
  }

  @SuppressWarnings("unused")
  public static void notifyToUI(final String title,
                                final String contentText,
                                final Context context) {
    final Notification.Builder builder = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground);

    getNotificationManager().notify(-1, setNotificationChannel(builder,
            getNotificationManager(),
            "UnlockMe Notification Channel",
            "Regular notification channel for UnlockMe",
            true).build());
  }

  public static Notification.Builder setNotificationChannel(
          final Notification.Builder builder,
          final NotificationManager notificationManager,
          final String channelID, final String desc,
          final boolean enableVibrationAndSound) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(channelID) == null) {
          NotificationChannel notificationChannel = new NotificationChannel(
                  channelID, channelID, NotificationManager.IMPORTANCE_DEFAULT);
          notificationChannel.setDescription(desc);
          notificationChannel.enableLights(true);
          notificationChannel.setShowBadge(true);
          if (!enableVibrationAndSound) {
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
          }
          notificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

          notificationManager.createNotificationChannel(notificationChannel);
        }
        builder
                .setChannelId(channelID)
                .setBadgeIconType(Notification.BADGE_ICON_LARGE);

      } else
        builder.setPriority(Notification.PRIORITY_HIGH);
      builder.setShowWhen(true);
    }
    return builder;
  }

  public static NotificationManager getNotificationManager() {
    if (notificationManager == null)
      notificationManager = (NotificationManager)
              MainActivity.applicationContext.
                      getSystemService(NOTIFICATION_SERVICE);
    return notificationManager;
  }
}
