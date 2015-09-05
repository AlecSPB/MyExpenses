package org.totschnig.myexpenses.service;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.util.Distrib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.vending.licensing.PreferenceObfuscator;

/**
 * This handler is used in two different scenarios:
 * 1) MyExpensesContrib calls MyExpenses.MyService when having retrieved the license status and posts a message to this handler
 * 2) MyExpenses.MyApplication onStartup calls MyExpensesContrib.MyService and sets this handler as replyTo to retrieve the license status
 * this handler is subclassed in MyApplication, so that we can handle unbinding from the service there
 */
public class UnlockHandler extends Handler {
  private static final int STATUS_TEMPORARY = 3;
  private static final int STATUS_PERMANENT = 4;
  private static final int STATUS_FINAL = 7;
  
  @Override
  public void handleMessage(Message msg) {
    MyApplication app = MyApplication.getInstance();
    if (Distrib.getContribStatusInfo(app).equals(Distrib.STATUS_ENABLED_LEGACY_SECOND)) {
      return;
    }
    Log.i(MyApplication.TAG,"Now handling answer from license verification service; got status "+msg.what);
    NotificationManager notificationManager =
        (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationCompat.Builder builder;
    Notification notification;
    switch(msg.what) {
      case STATUS_FINAL:
        doUnlock();
        break;
      case STATUS_TEMPORARY:
      case STATUS_PERMANENT:
        if (!BuildConfig.FLAVOR_distribution.equals("play")) {
          doUnlock();
        } else {
          builder =
              new NotificationCompat.Builder(app)
                  .setSmallIcon(R.drawable.ic_home_dark)
                  .setContentTitle(app.getString(R.string.licence_validation_failure))
                  .setContentText("Please upgrade My Expenses Contrib to version 1.5")
                  .setContentIntent(PendingIntent.getActivity(app, 0, new Intent(app, MyExpenses.class), 0));
          notification = builder.build();
          notification.flags = Notification.FLAG_AUTO_CANCEL;
          notificationManager.notify(0, notification);
        }
        break;
    }
  }

  private void doUnlock() {
    MyApplication app = MyApplication.getInstance();
    NotificationManager notificationManager =
        (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
    PreferenceObfuscator mPreferences = Distrib.getLicenseStatusPrefs(app);
    app.setContribStatus(Distrib.STATUS_ENABLED_LEGACY_SECOND);
    mPreferences.putString(MyApplication.PrefKey.LICENSE_STATUS.getKey(), String.valueOf(Distrib.STATUS_ENABLED_LEGACY_SECOND));
    mPreferences.commit();
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(app)
          .setSmallIcon(R.drawable.ic_home_dark)
          .setContentTitle(app.getString(R.string.licence_validation_premium))
          .setContentText(app.getString(R.string.thank_you))
          .setContentIntent(PendingIntent.getActivity(app, 0, new Intent(app, MyExpenses.class), 0));
    Notification notification  = builder.build();
    notification.flags = Notification.FLAG_AUTO_CANCEL;
    notificationManager.notify(0, notification);
  }
}
