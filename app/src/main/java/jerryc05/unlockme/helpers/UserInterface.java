package jerryc05.unlockme.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Build;

import jerryc05.unlockme.MainActivity;
import jerryc05.unlockme.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public abstract class UserInterface {

  private static NotificationManager notificationManager;

  @SuppressWarnings("unused")
  public static void throwExceptionToDialog(final Activity activity, final Exception e) {
    if (activity == null)
      return;

    throwExceptionToDialog(activity, e, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        throw new UnsupportedOperationException(e);
      }
    });
  }

  @SuppressWarnings("WeakerAccess")
  public static void throwExceptionToDialog(final Activity activity, final Exception e,
                                            final DialogInterface.OnClickListener onClickListener) {
    if (activity == null)
      return;

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(activity)
                .setTitle("Crash Report")
                .setMessage(e.toString())
                .setIcon(R.drawable.ic_round_error_24px)
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }

  public static void throwExceptionToNotification(final String contentText,
                                                  final String subText) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle("Crash Report")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_shield_foreground)
            .setSubText(subText)
            .setStyle(new Notification.BigTextStyle()
                    .bigText(contentText));

    notificationManager.notify(-1, setNotificationChannel(builder,
            getNotificationManager(),
            "Crash Report",
            "Crash report notification channel for UnlockMe",
            true).build());

    throw new UnsupportedOperationException(contentText);
  }

  @SuppressWarnings("unused")
  public static void notifyPictureToUI(final String contentText,
                                       final byte[] bytes) {
    final Notification.Builder builder = new Notification.Builder(
            MainActivity.applicationContext)
            .setContentTitle("Picture Taken")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_shield_foreground)
            .setStyle(new Notification.BigPictureStyle()
                    .bigPicture(BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length)));

    getNotificationManager().notify(-1, setNotificationChannel(builder,
            getNotificationManager(),
            "Image Captured Report",
            "Image captured report notification channel for UnlockMe",
            false).build());
  }

  private static Notification.Builder setNotificationChannel(
          final Notification.Builder builder,
          final NotificationManager notificationManager,
          final String channelID, final String desc, final boolean vibration) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(channelID) == null) {
          NotificationChannel notificationChannel = new NotificationChannel(
                  channelID, channelID, NotificationManager.IMPORTANCE_DEFAULT);
          notificationChannel.setDescription(desc);
          notificationChannel.enableLights(true);
          notificationChannel.enableVibration(vibration);
          notificationChannel.setShowBadge(true);
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

  private static NotificationManager getNotificationManager() {
    if (notificationManager == null)
      notificationManager = (NotificationManager)
              MainActivity.applicationContext.
                      getSystemService(NOTIFICATION_SERVICE);
    return notificationManager;
  }
}
