package jerryc05.unlockme.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import jerryc05.unlockme.R;
import jerryc05.unlockme.services.ForegroundService;

import static android.content.Context.NOTIFICATION_SERVICE;
import static jerryc05.unlockme.services.ForegroundService.ACTION_DISMISS_NOTIFICATION;
import static jerryc05.unlockme.services.ForegroundService.EXTRA_CANCEL_NOTIFICATION_ID;

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
@UiThread
public final class UserInterface {

  @SuppressWarnings("unused")
  public static void showExceptionToDialog(
          @NonNull final Exception e, @NonNull final Context context) {
    showExceptionToDialog(e, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        throw new UnsupportedOperationException(e);
      }
    }, context);
  }

  @SuppressWarnings("WeakerAccess")
  public static void showExceptionToDialog(
          @NonNull final Exception e,
          @NonNull final OnClickListener onClickListener,
          @NonNull final Context context) {
    if (!(context instanceof Activity))
      return;

    ((Activity) context).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(context)
                .setTitle("Crash Report")
                .setMessage(e.toString())
                .setIcon(R.drawable.ic_round_error)
                .setCancelable(false)
                .setPositiveButton("OK", onClickListener)
                .show();
      }
    });
  }

  public static void showExceptionToNotification(
          @NonNull final String contentText, @Nullable final String subText,
          @NonNull final Context context) {
    showExceptionToNotificationNoRethrow(contentText, subText, context);
    throw new UnsupportedOperationException(contentText);
  }

  public static void showExceptionToNotificationNoRethrow(
          @NonNull final String contentText, @Nullable final String subText,
          @NonNull final Context context) {
    final String title = "Crash Report";
    final int    id    = title.hashCode();

    final Builder builder = new Builder(context)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground)
            .setSubText(subText)
            .setStyle(new Notification.BigTextStyle()
                    .bigText(contentText))
            .setContentIntent(getUpdateNotificationPendingIntent(id, context));

    getNotificationManager(context).notify(id, setNotificationChannel(
            builder, getNotificationManager(context),
            "Crash Report",
            "Crash report notification channel for UnlockMe",
            true).build());
  }

  @SuppressWarnings("unused")
  public static void notifyPictureToUI(@NonNull final String contentText,
                                       @NonNull final byte[] bytes,
                                       @NonNull final Context context) {
    final String title = "Picture Taken";
    final int    id    = title.hashCode();

    final Builder builder = new Builder(context)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground)
            .setStyle(new Notification.BigPictureStyle()
                    .bigPicture(BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length)))
            .setContentIntent(getUpdateNotificationPendingIntent(id, context));

    getNotificationManager(context).notify(id, setNotificationChannel(
            builder, getNotificationManager(context),
            "Image Captured Report",
            "Image captured report notification channel for UnlockMe",
            false).build());
  }

  /**
   * @param cancelNotificationID pass -1 to stop dismissing notification.
   */
  private static PendingIntent getUpdateNotificationPendingIntent(
          final int cancelNotificationID,
          @NonNull final Context context) {

    final Intent intent = new Intent(context, ForegroundService.class);
    intent.setAction(ACTION_DISMISS_NOTIFICATION);
    intent.putExtra(EXTRA_CANCEL_NOTIFICATION_ID, cancelNotificationID);

    final PendingIntent pendingIntent;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
      pendingIntent = PendingIntent.getService(context,
              ForegroundService.class.getSimpleName().hashCode(),
              intent, PendingIntent.FLAG_UPDATE_CURRENT);
    else
      pendingIntent = PendingIntent.getForegroundService(context,
              ForegroundService.class.getSimpleName().hashCode(),
              intent, PendingIntent.FLAG_UPDATE_CURRENT);
    return pendingIntent;
  }

  public static void notifyToForegroundService(@NonNull final Service service) {
    final String title = "Background Service";

    final Builder builder = new Builder(service)
            .setContentTitle(title)
            .setSmallIcon(
                    R.drawable.ic_launcher_smartphone_lock_foreground);

    service.startForeground(title.hashCode(),
            UserInterface.setNotificationChannel(builder,
                    getNotificationManager(service), title,
                    "Background service notification for UnlockMe.",
                    true).build());
  }

  @SuppressWarnings("unused")
  public static void notifyToUI(@NonNull final String title,
                                @NonNull final String contentText,
                                @NonNull final Context context) {
    final int id = title.hashCode();

    @SuppressLint("IconColors") final Builder builder = new Builder(context)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_smartphone_lock_foreground)
            .setAutoCancel(true); //todo auto cancel

    getNotificationManager(context).notify(id, setNotificationChannel(
            builder, getNotificationManager(context),
            "UnlockMe Notification Channel",
            "Regular notification channel for UnlockMe",
            true).build());
  }

  @SuppressWarnings("WeakerAccess")
  static Builder setNotificationChannel(
          @NonNull final Builder builder,
          @NonNull final NotificationManager notificationManager,
          @NonNull final String channelID, @Nullable final String desc,
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

  public static NotificationManager getNotificationManager(
          @NonNull final Context context) {
    return (NotificationManager)
            context.getSystemService(NOTIFICATION_SERVICE);
  }
}
