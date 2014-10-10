package com.jedga.peek.free.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.jedga.peek.free.NotificationPeek;
import com.jedga.peek.free.R;
import com.jedga.peek.free.helpers.PanelHelper;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

public class ListenerService extends NotificationListenerService {

    public static final String SHOW_NOTIFICATION = "com.jedga.peek.SHOW_NOTIFICATION";
    public static final String REMOVE_NOTIFICATION = "com.jedga.peek.REMOVE_NOTIFICATION";
    public static final String REMOVE_NOTIFICATION_FROM_STATUSBAR = "com.jedga.peek.REMOVE_NOTIFICATION_FROM_STATUSBAR";

    private static final int PEEK_SERVICE_NOTIFICATION_ID = 1;

    private NotificationPeek mNotificationPeek;
    private ArrayDeque<StatusBarNotification> mAddQueue;
    private ArrayDeque<StatusBarNotification> mRemoveQueue;

    private boolean mHasLoadedNotifications;
    private CommandReceiver mCommandReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationPeek = new NotificationPeek(this);
        mAddQueue = new ArrayDeque<StatusBarNotification>();
        mRemoveQueue = new ArrayDeque<StatusBarNotification>();
        mCommandReceiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SHOW_NOTIFICATION);
        filter.addAction(REMOVE_NOTIFICATION);
        filter.addAction(REMOVE_NOTIFICATION_FROM_STATUSBAR);
        registerReceiver(mCommandReceiver, filter);

        String androidVersion = android.os.Build.VERSION.RELEASE;
        boolean showPersistentNotification = androidVersion.equals("4.4")
                || androidVersion.equals("4.4.1") || androidVersion.equals("4.4.2");

        if (showPersistentNotification) {
            Notification.Builder notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.peek_service_running))
                    .setTicker(getString(R.string.peek_service_running));
            notification.setPriority(Notification.PRIORITY_MIN);

            startForeground(PEEK_SERVICE_NOTIFICATION_ID, notification.build());
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000, restartServicePI);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mCommandReceiver);

        mNotificationPeek.unregisterEventReceivers();
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        if (!mHasLoadedNotifications) {
            // load all the notifications for the first time
            mHasLoadedNotifications = true;
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                if (notifications.length > 0) {
                    for (int i = 0; i < notifications.length; i++) {
                        try {
                            if (!PanelHelper.getContentDescription(sbn)
                                    .equals(PanelHelper.getContentDescription(notifications[i]))
                                    && !(notifications[i].getPackageName().equals(getPackageName())
                                    && !notifications[i].isClearable())) {
                                mNotificationPeek.addNotification(notifications[i]);
                            }
                        } catch (Exception e) {
                            // fuck you binder
                        }
                    }
                }
            }
        }

        if (sbn.getPackageName().equals(getPackageName()) && !sbn.isClearable()) return;
        mAddQueue.push(sbn);
        Intent i = new Intent(ListenerService.SHOW_NOTIFICATION);
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(final StatusBarNotification sbn) {
        mRemoveQueue.push(sbn);
        Intent i = new Intent(ListenerService.REMOVE_NOTIFICATION);
        sendBroadcast(i);
    }

    // This class receives commands from the notification listener background
    // thread and passes to the UI thread. Yes, I'm fabulous
    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(SHOW_NOTIFICATION)) {
                StatusBarNotification sbn = null;
                try {
                    sbn = mAddQueue.pop();
                } catch (NoSuchElementException nsee) {
                    // ups!, bye
                }
                if (sbn == null) return;
                mNotificationPeek.showNotification(sbn);
                if (mNotificationPeek.isShowing()) {
                    mNotificationPeek.updateNotificationIcons();
                }
            } else if (intent.getAction().equals(REMOVE_NOTIFICATION)) {
                StatusBarNotification sbn = null;
                try {
                    sbn = mRemoveQueue.pop();
                } catch (NoSuchElementException nsee) {
                    // ups!, bye
                }
                if (sbn == null) return;
                mNotificationPeek.removeNotification(sbn);
                if (mNotificationPeek.isShowing()) {
                    mNotificationPeek.updateNotificationIcons();
                }
            } else if (intent.getAction().equals(REMOVE_NOTIFICATION_FROM_STATUSBAR)) {
                if (intent.getStringExtra("package") != null) {
                    String packageName = intent.getStringExtra("package");
                    String tag = intent.getStringExtra("tag");
                    int id = intent.getIntExtra("id", 0);
                    cancelNotification(packageName, tag, id);
                }
            }
        }
    }
}
