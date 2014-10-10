package com.jedga.peek.free;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jedga.peek.free.helpers.PanelHelper;
import com.jedga.peek.free.helpers.PreferencesHelper;
import com.jedga.peek.free.helpers.SensorActivityHandler;
import com.jedga.peek.free.helpers.WallpaperHelper;
import com.jedga.peek.free.layout.GestureDots;
import com.jedga.peek.free.layout.NotificationLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class NotificationPeek implements SensorActivityHandler.SensorChangedCallback {

    public final static boolean DEBUG = true;
    private final static String TAG = "NotificationPeek_DEBUG";
    private static final float ICON_LOW_OPACITY = 0.3f;

    private static RelativeLayout sPeekView;
    private static GestureDots sRightDots;
    private static GestureDots sLeftDots;
    private static ImageView sBottomIcon;
    private static TextView sClockView;
    private static TextView sDateView;
    private static WallpaperHelper sWallpaperHelper;
    private static PowerManager.WakeLock sPartialWakeLock;
    private static Handler sHandler;
    private static Context sContext;
    private static NotificationClicker sClickedItem;
    private static StatusBarNotification sCurrentNotification;
    private static boolean sShouldAnimateTransition;
    private static boolean sBlockNextScreenOffIntent;
    private static boolean sExpanded;
    private static boolean sShowing;

    private LinearLayout mNotificationView;
    private LinearLayout mNotificationsContainer;
    private ImageView mNotificationIcon;
    private TextView mNotificationTitle;
    private LinearLayout mNotificationExpanded;
    private SensorActivityHandler mSensorHandler;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private static PowerManager.WakeLock sScreenOnWakelock;
    private Runnable mPartialWakeLockRunnable;
    private Handler mWakeLockHandler;
    private List<StatusBarNotification> mShownNotifications
            = new ArrayList<StatusBarNotification>();
    private StatusBarNotification mNextNotification;
    private int mNotificationCount;
    private boolean mRingingOrConnected;
    private boolean mEnabled;
    private boolean mAnimating;

    private boolean mEventsRegistered = true;

    public NotificationPeek(Context context) {
        mSensorHandler = new SensorActivityHandler(context, this, false);
        sWallpaperHelper = new WallpaperHelper(context);

        sContext = context;
        sHandler = new Handler();
        mWakeLockHandler = new Handler();

        updateStatus();

        mPartialWakeLockRunnable = new Runnable() {
            @Override
            public void run() {
                // After PARTIAL_WAKELOCK_TIME with no user interaction, release CPU wakelock
                // and unregister event listeners.
                if (sPartialWakeLock.isHeld()) {
                    if (mEventsRegistered) {
                        if (DEBUG) Log.d(TAG, "Removing event listeners");
                        mSensorHandler.unregisterEventListeners();
                        mEventsRegistered = false;
                    }
                    sPartialWakeLock.release();
                }
                // Also, unregister screen on wakelock, if held
                if (sScreenOnWakelock.isHeld()) {
                    sScreenOnWakelock.release();
                }
            }
        };

        mKeyguardManager = (KeyguardManager) sContext.getSystemService(Context.KEYGUARD_SERVICE);
        mPowerManager = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
        sPartialWakeLock = mPowerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName() + "_partial");
        sScreenOnWakelock = mPowerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                getClass().getSimpleName() + "_screen_on");

        TelephonyManager telephonyManager = (TelephonyManager)
                sContext.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new CallStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        // build the layout
        sPeekView = new RelativeLayout(sContext) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_MOVE) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        sHandler.removeCallbacksAndMessages(null);
                    }
                }
                if (action == MotionEvent.ACTION_UP) scheduleTasks();
                return super.onInterceptTouchEvent(event);
            }
        };
        sPeekView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }

            private GestureDetector gestureDetector
                    = new GestureDetector(sContext, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    Intent i = new Intent(PeekActivity.TURN_SCREEN_OFF);
                    sContext.sendBroadcast(i);
                    return super.onDoubleTap(e);
                }
            });
        });

        LayoutTransition transition = new LayoutTransition();
        transition.disableTransitionType(LayoutTransition.CHANGING);
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        sPeekView.setLayoutTransition(transition);

        // define icon background size to start adjusting layout placement
        int backgroundSize = sContext.getResources()
                .getDimensionPixelSize(R.dimen.notification_icon_background_size);

        // layout dots
        sRightDots = new GestureDots(sContext, 4);
        sRightDots.createDots(sPeekView, backgroundSize, 1, true);
        sRightDots.setActionResource(
                PreferencesHelper.useReversedGestures(sContext)
                        ? R.drawable.ic_unlock : R.drawable.ic_dismiss
        );

        sLeftDots = new GestureDots(sContext, 4);
        sLeftDots.createDots(sPeekView, backgroundSize, 6, false);
        sLeftDots.setActionResource(
                PreferencesHelper.useReversedGestures(sContext)
                        ? R.drawable.ic_dismiss : R.drawable.ic_unlock
        );

        // root view
        NotificationLayout rootView = new NotificationLayout(sContext);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setViewBridge(this);
        rootView.setId(1);

        sPeekView.addView(rootView);

        RelativeLayout.LayoutParams rootLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rootLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        rootView.setLayoutParams(rootLayoutParams);

        // clock
        sClockView = new TextView(sContext);
        sClockView.setTextColor(Color.WHITE);
        sClockView.setGravity(Gravity.CENTER);
        sClockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 65);
        sClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        sClockView.setId(2);

        RelativeLayout.LayoutParams relativeLayoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        relativeLayoutParams.topMargin =
                sContext.getResources().getDimensionPixelSize(R.dimen.clock_date_padding);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        sClockView.setLayoutParams(relativeLayoutParams);
        sPeekView.addView(sClockView);

        // date
        sDateView = new TextView(sContext);
        sDateView.setTextColor(Color.WHITE);
        sDateView.setGravity(Gravity.CENTER);
        sDateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        sDateView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        relativeLayoutParams =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        relativeLayoutParams.addRule(RelativeLayout.BELOW, sClockView.getId()); // below clock
        sDateView.setLayoutParams(relativeLayoutParams);
        sPeekView.addView(sDateView);

        // notification container
        mNotificationView = new LinearLayout(sContext);
        mNotificationView.setOrientation(LinearLayout.VERTICAL);
        mNotificationView.setId(3);
        rootView.addView(mNotificationView);

        // current notification ticker
        mNotificationTitle = new TextView(sContext);
        mNotificationTitle.setTextColor(Color.WHITE);
        mNotificationTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        mNotificationTitle.setGravity(Gravity.CENTER);
        mNotificationTitle.setEllipsize(TextUtils.TruncateAt.END);
        mNotificationTitle.setSingleLine(true);
        mNotificationTitle.setId(4);

        sPeekView.addView(mNotificationTitle);

        RelativeLayout.LayoutParams textLayoutParams
                = new RelativeLayout.LayoutParams(sContext.getResources()
                .getDimensionPixelSize(R.dimen.notification_text_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        textLayoutParams.addRule(RelativeLayout.BELOW, rootView.getId());
        mNotificationTitle.setLayoutParams(textLayoutParams);

        // expanded content
        mNotificationExpanded = new LinearLayout(sContext);
        mNotificationExpanded.setVisibility(View.GONE);
        sPeekView.addView(mNotificationExpanded);

        //
        RelativeLayout.LayoutParams expandedLayoutParams
                = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                sContext.getResources().getDimensionPixelSize(R.dimen.notification_min_height)
        );
        expandedLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        expandedLayoutParams.addRule(RelativeLayout.ABOVE, rootView.getId());
        expandedLayoutParams.bottomMargin = sContext
                .getResources().getDimensionPixelSize(R.dimen.item_padding);
        mNotificationExpanded.setLayoutParams(expandedLayoutParams);

        // current notification icon
        mNotificationIcon = new ImageView(sContext);
        mNotificationIcon.setBackgroundResource(R.drawable.ring);
        mNotificationIcon.setOnTouchListener(PanelHelper.getHighlightTouchListener());
        mNotificationIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mNotificationView.addView(mNotificationIcon);

        LinearLayout.LayoutParams linearLayoutParams
                = new LinearLayout.LayoutParams(backgroundSize, backgroundSize);
        linearLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        mNotificationIcon.setLayoutParams(linearLayoutParams);

        // notification icons
        mNotificationsContainer = new LinearLayout(sContext) {
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                int action = ev.getAction();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_MOVE) {
                    StatusBarNotification n = getNotificationFromEvent(ev);
                    if (n != null) {
                        updateSelection(n, false);
                    }
                }
                return true;
            }
        };
        mNotificationsContainer.setOrientation(LinearLayout.HORIZONTAL);
        mNotificationsContainer.setPadding(0, sContext.getResources()
                .getDimensionPixelSize(R.dimen.item_padding) * 2, 0, 0);
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.enableTransitionType(LayoutTransition.CHANGING);
        transitioner.disableTransitionType(LayoutTransition.DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.APPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        transitioner.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        mNotificationsContainer.setLayoutTransition(transitioner);
        mNotificationsContainer.setId(5);

        sPeekView.addView(mNotificationsContainer);

        RelativeLayout.LayoutParams notificationsLayoutParams
                = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        notificationsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        notificationsLayoutParams.addRule(RelativeLayout.BELOW, mNotificationTitle.getId());
        mNotificationsContainer.setLayoutParams(notificationsLayoutParams);

        sBottomIcon = new ImageView(sContext);
        sBottomIcon.setAlpha(0f);
        sBottomIcon.setImageResource(R.drawable.ic_open);
        sBottomIcon.setPadding(0, sContext.getResources()
                .getDimensionPixelSize(R.dimen.item_padding) * 2, 0, 0);

        sPeekView.addView(sBottomIcon);

        notificationsLayoutParams
                = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        notificationsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        notificationsLayoutParams.addRule(RelativeLayout.BELOW, mNotificationsContainer.getId());
        sBottomIcon.setLayoutParams(notificationsLayoutParams);

    }

    public static void animateDots(boolean show, boolean animateRight, boolean animateLeft) {
        if (show) {
            if (animateRight) sRightDots.animateIn();
            if (animateLeft) sLeftDots.animateIn();
        } else {
            if (animateRight) sRightDots.animateOut();
            if (animateLeft) sLeftDots.animateOut();
        }
    }

    public static void toggleBottomIcon(boolean show) {
        sBottomIcon.animate().alpha(show ? 1f : 0f).setListener(null);
    }

    private static void scheduleTasks() {
        sHandler.removeCallbacksAndMessages(null);

        int screenOnTime = PreferencesHelper.getScreenTimeout(sContext) * 1000;

        // turn screen off
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(PeekActivity.TURN_SCREEN_OFF);
                sContext.sendBroadcast(i);
            }
        }, screenOnTime);
    }

    public static void dismissNotification() {
        if (sShowing) {
            sShowing = false;
            sPeekView.animate().alpha(0f).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (DEBUG) Log.d(TAG, "Dismissing view");
                            if (sPartialWakeLock.isHeld()) {
                                if (DEBUG) Log.d(TAG, "Releasing partial wakelock");
                                sPartialWakeLock.release();
                            }
                            if (sScreenOnWakelock.isHeld()) {
                                if (DEBUG) Log.d(TAG, "Releasing screen on wakelock");
                                sScreenOnWakelock.release();
                            }
                            sPeekView.setVisibility(View.GONE);
                            animateDots(false, true, true);

                            Intent i = new Intent(PeekActivity.FINISH_ACTIVITY);
                            sContext.sendBroadcast(i);
                        }
                    }
            );
        }
    }

    public static void dismissKeyguardAndNotification() {
        Intent i = new Intent(PeekActivity.DISMISS_KEYGUARD);
        sContext.sendBroadcast(i);
        dismissNotification();
    }

    private static CharSequence getCurrentTimeText() {
        boolean b24 = DateFormat.is24HourFormat(sContext);
        int res;
        if (b24) {
            res = R.string.twenty_four_hour_time_format;
        } else {
            res = R.string.twelve_hour_time_format;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        String format = sContext.getString(res);
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(calendar.getTime()).toUpperCase();
    }

    private static String getCurrentDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d");
        return sdf.format(new Date()).toUpperCase();
    }

    private boolean isKeyguardSecureShowing() {
        return mKeyguardManager.isKeyguardLocked() && mKeyguardManager.isKeyguardSecure();
    }

    public void updateStatus() {
        mEnabled = PreferencesHelper.isPeekEnabled(sContext);
        sShouldAnimateTransition = false;
        if (mEnabled) {
            mSensorHandler.registerScreenReceiver();
        } else {
            sHandler.removeCallbacksAndMessages(null);
            mSensorHandler.unregisterScreenReceiver();
            mSensorHandler.unregisterEventListeners();
        }

        // update drawables
        if (sRightDots != null && sLeftDots != null) {
            sRightDots.setActionResource(
                    PreferencesHelper.useReversedGestures(sContext)
                            ? R.drawable.ic_unlock : R.drawable.ic_dismiss
            );
            sLeftDots.setActionResource(
                    PreferencesHelper.useReversedGestures(sContext)
                            ? R.drawable.ic_dismiss : R.drawable.ic_unlock
            );
        }
    }

    public View getNotificationView() {
        return mNotificationView;
    }

    public void setAnimating(boolean animating) {
        mAnimating = animating;
        mNotificationExpanded.animate().setListener(null);
        boolean visible = !isKeyguardSecureShowing()
                || PreferencesHelper.showNotificationContentSecure(sContext);
        int height = sContext.getResources()
                .getDimensionPixelSize(R.dimen.notification_min_height);
        if (animating) {
            mNotificationExpanded.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                sClockView.animate().translationYBy(-sClockView.getHeight()).alpha(0f);
                sDateView.animate().translationYBy(-sDateView.getHeight()).alpha(0f);
                mNotificationExpanded.setTranslationY(height);
                mNotificationExpanded.setAlpha(0f);
                mNotificationExpanded.animate()
                        .translationYBy(-height).alpha(1f);
                mNotificationTitle.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mNotificationTitle.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                });
                mNotificationsContainer.animate().alpha(0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mNotificationsContainer.setVisibility(View.GONE);
                                super.onAnimationEnd(animation);
                            }
                        });
            }
        } else {
            if (visible) {
                sClockView.animate().translationY(0).alpha(1f);
                sDateView.animate().translationY(0).alpha(1f);
                sBottomIcon.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mNotificationTitle.setVisibility(View.VISIBLE);
                        mNotificationTitle.animate().alpha(1f).setListener(null);
                        mNotificationsContainer.setVisibility(View.VISIBLE);
                        mNotificationsContainer.animate().alpha(1f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        // toggle notification after last animation has ended
                                        // to keep performance.
                                        toggleNotificationExpanded(false);
                                        super.onAnimationEnd(animation);
                                    }
                                });
                        super.onAnimationEnd(animation);
                    }
                });
                mNotificationExpanded.animate().translationYBy(height).alpha(0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mNotificationExpanded.setVisibility(View.GONE);
                                super.onAnimationEnd(animation);
                            }
                        });
            }
        }
    }

    public void showNotification(StatusBarNotification n) {
        synchronized (this) {
            showNotification(n, false);
        }
    }

    private void showNotification(StatusBarNotification n, boolean force) {
        if (DEBUG) Log.d(TAG, "Showing notification: " + PanelHelper.getContentDescription(n));
        if (n == null) return;
        boolean shouldDisplay = shouldDisplayNotification(n) || force;
        addNotification(n);

        updateStatus(); // we can't register listeners on different processes, so update locally

        if (!mEnabled /* peek is disabled */
                || (mPowerManager.isScreenOn() && !sShowing) /* no peek when screen is on */
                || !shouldDisplay /* notification has already been displayed */
                || mRingingOrConnected /* is phone ringing? */
                || PreferencesHelper
                .isPackageBlackListed(sContext, n.getPackageName() /* blacklist */)) return;

        if (isNotificationActive(n)) {
            final boolean selfPackage = n.getPackageName().equals(sContext.getPackageName());

            // update information
            updateNotificationIcons();
            updateSelection(n, false);

            if (PreferencesHelper.usePowerOnNotifications(sContext)) {
                if (PreferencesHelper.shouldWakeUp(sContext) && !mSensorHandler.isInPocket()) {
                    // show immediately
                    startNotificationActivity(!selfPackage, true);
                    return;
                } else {
                    // show next time user presses power button
                    startNotificationActivity(!selfPackage, false);
                }
            }

            // check if phone is in the pocket or lying on a table
            if (mSensorHandler.isInPocket()
                    || (mSensorHandler.isOnTable()
                    && !PreferencesHelper.shouldWakeUp(sContext))) {
                if (DEBUG) Log.d(TAG, "Queueing notification");

                // use partial wakelock to get sensors working
                if (sPartialWakeLock.isHeld()) {
                    if (DEBUG) Log.d(TAG, "Releasing partial wakelock");
                    sPartialWakeLock.release();
                }

                if (DEBUG) Log.d(TAG, "Acquiring partial wakelock");
                sPartialWakeLock.acquire();
                if (!mEventsRegistered) {
                    mSensorHandler.registerEventListeners();
                    mEventsRegistered = true;
                }

                mWakeLockHandler.removeCallbacks(mPartialWakeLockRunnable);
                int detectionType = PreferencesHelper.getDetectionType(sContext);
                boolean shouldStopPolling = false;
                switch (detectionType) {
                    case PreferencesHelper.TYPE_BATTERY_SAVING:
                        shouldStopPolling = true;
                        break;
                    case PreferencesHelper.TYPE_ALWAYS_ACTIVE:
                        shouldStopPolling = false;
                        break;
                    case PreferencesHelper.TYPE_ACTIVE_WHILE_CHARGING:
                        shouldStopPolling = !mSensorHandler.isPhoneCharging();
                        break;
                }

                if (shouldStopPolling) {
                    int sensorPollingTime = PreferencesHelper.getPollingTime(sContext) * 1000;
                    mWakeLockHandler.postDelayed(mPartialWakeLockRunnable, sensorPollingTime);
                }

                mNextNotification = n;
                return;
            }

            if (n.isClearable()) {
                mWakeLockHandler.removeCallbacks(mPartialWakeLockRunnable);

                startNotificationActivity(!selfPackage, true);
            }
        }
    }

    private void startNotificationActivity(boolean clearTask, boolean wakeUp) {
        if (sShowing) {
            if (wakeUp) {
                Intent i = new Intent(PeekActivity.TURN_SCREEN_ON);
                sContext.sendBroadcast(i);
            }
        } else {
            if (DEBUG) Log.d(TAG, "Starting notification activity with flags: clearTask: "
                    + clearTask + " wakeUp: " + wakeUp);
            sShowing = true;
            Intent intent = new Intent(sContext, PeekActivity.class);
            intent.putExtra(PeekActivity.WAKE_UP, wakeUp);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (clearTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            sContext.startActivity(intent);
        }
    }

    public void addNotification(StatusBarNotification n) {
        try {
            for (int i = 0; i < mShownNotifications.size(); i++) {
                if (PanelHelper.getContentDescription(n).equals(
                        PanelHelper.getContentDescription(mShownNotifications.get(i)))) {
                    mShownNotifications.set(i, n);
                    return;
                }
            }
            mNotificationCount++;
            mShownNotifications.add(n);
        } catch (Exception e) {
            //fuck you binder - DeadObjectException
        }
    }

    public void removeNotification(StatusBarNotification n) {
        removeNotification(PanelHelper.getContentDescription(n));
    }

    public void removeNotification(String contentDescription) {
        try {
            int oldSize = mShownNotifications.size();
            for (int i = 0; i < mShownNotifications.size(); i++) {
                String notification
                        = PanelHelper.getContentDescription(mShownNotifications.get(i));
                // notification was removed, if it was intended to be the next, cancel that.
                if (notification.equals(PanelHelper.getContentDescription(mNextNotification))) {
                    mNextNotification = null;
                }
                if (contentDescription.equals(notification)) {
                    mShownNotifications.remove(i);
                    i--;
                }
            }

            if (mNotificationCount > 0
                    && mShownNotifications.size() < oldSize) {
                mNotificationCount--;
            }
        } catch (Exception e) {
            //fuck you binder - DeadObjectException
        }
    }

    public void updateNotificationIcons() {
        mNotificationsContainer.removeAllViews();
        int iconSize = sContext.getResources()
                .getDimensionPixelSize(R.dimen.small_notification_icon_size);
        int padding = sContext.getResources()
                .getDimensionPixelSize(R.dimen.small_notification_icon_padding);
        Object tag = mNotificationView.getTag();
        String currentNotification = tag != null ? tag.toString() : null;
        boolean foundCurrentNotification = false;
        int notificationCount = mNotificationCount;
        for (int i = 0; i < mNotificationCount; i++) {
            final StatusBarNotification n = mShownNotifications.get(i);
            // don't show blacklisted apps
            if (PreferencesHelper.isPackageBlackListed(sContext, n.getPackageName())) {
                notificationCount--;
                continue;
            }
            ImageView icon = new ImageView(sContext);
            if (n.toString().equals(currentNotification)) {
                foundCurrentNotification = true;
            } else {
                icon.setAlpha(ICON_LOW_OPACITY);
            }
            icon.setPadding(padding, 0, padding, 0);
            icon.setImageDrawable(getIconFromResource(n));
            icon.setTag(n);
            mNotificationsContainer.addView(icon);
            LinearLayout.LayoutParams linearLayoutParams
                    = new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(linearLayoutParams);
        }
        mNotificationsContainer.setVisibility(View.VISIBLE);
        if (notificationCount <= 1) {
            mNotificationsContainer.setVisibility(View.GONE);
        }
        if (!foundCurrentNotification) {
            boolean foundNextNotification = false;
            if (notificationCount > 0) {
                for (StatusBarNotification n : mShownNotifications) {
                    if (!PreferencesHelper.isPackageBlackListed(sContext, n.getPackageName())) {
                        foundNextNotification = true;
                        updateSelection(n, false);
                    }
                }
            }

            if (notificationCount < 0 || !foundNextNotification) {
                // turn screen off if this is the last notification
                dismissNotification(); // force removal
                Intent i = new Intent(PeekActivity.TURN_SCREEN_OFF);
                sContext.sendBroadcast(i);
            }
        }
    }

    private void updateSelection(StatusBarNotification n, boolean expand) {
        String oldNotif = PanelHelper.getContentDescription(
                (StatusBarNotification) mNotificationView.getTag());
        String newNotif = PanelHelper.getContentDescription(n);
        boolean sameNotification = newNotif.equals(oldNotif);
        if (!mAnimating || sameNotification) {
            if(!expand) {
                sCurrentNotification = n;
                mNotificationIcon.setImageDrawable(getIconFromResource(n));
                if (n != null) {
                    final View.OnClickListener listener = new NotificationClicker(n);
                    mNotificationIcon.setOnClickListener(listener);
                } else {
                    mNotificationIcon.setOnClickListener(null);
                }
                mNotificationTitle.setText(getNotificationTickerText(n));
                mNotificationTitle.setVisibility((isKeyguardSecureShowing() &&
                        !PreferencesHelper
                                .showNotificationTitleSecure(sContext)) ? View.GONE : View.VISIBLE);
            }

            if (n != null) {
                Notification noti = n.getNotification();

                if (sExpanded && noti.bigContentView != null) {
                    mNotificationExpanded.getLayoutParams().height
                            = ViewGroup.LayoutParams.WRAP_CONTENT;
                    View notification
                            = noti.bigContentView.apply(sContext, mNotificationExpanded);
                    mNotificationExpanded.removeAllViews();
                    mNotificationExpanded.addView(notification);
                    PanelHelper.applyStyle(sContext, mNotificationExpanded);
                } else if (!sExpanded && noti.contentView != null) {
                    mNotificationExpanded.getLayoutParams().height
                            = sContext.getResources()
                            .getDimensionPixelSize(R.dimen.notification_min_height);
                    View notification
                            = noti.contentView.apply(sContext, mNotificationExpanded);
                    mNotificationExpanded.removeAllViews();
                    mNotificationExpanded.addView(notification);
                    PanelHelper.applyStyle(sContext, mNotificationExpanded);
                }

                mNotificationView.setTag(n);
            }

            mNotificationView.setAlpha(1f);
            mNotificationView.setX(0);
        }

        if(expand) return;

        // update small icons
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            ImageView view = (ImageView) mNotificationsContainer.getChildAt(i);
            if ((mAnimating ? oldNotif : newNotif).equals(PanelHelper
                    .getContentDescription((StatusBarNotification) view.getTag()))) {
                view.setAlpha(1f);
            } else {
                view.setAlpha(ICON_LOW_OPACITY);
            }
        }
    }

    public void toggleNotificationExpanded(boolean expanded) {
        sExpanded = expanded;
        updateSelection(sCurrentNotification, true);
    }

    public void openNotificationIntent() {
        mNotificationIcon.performClick();
    }

    public boolean isShowing() {
        return sShowing;
    }

    private boolean isNotificationActive(StatusBarNotification n) {
        for (StatusBarNotification mShownNotification : mShownNotifications) {
            if (PanelHelper.getContentDescription(n).equals(
                    PanelHelper.getContentDescription(mShownNotification))) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldDisplayNotification(StatusBarNotification n) {
        for (StatusBarNotification shown : mShownNotifications) {
            if (PanelHelper.getContentDescription(n).equals(
                    PanelHelper.getContentDescription(shown))) {
                return PanelHelper
                        .shouldDisplayNotification(shown, n);
            }
        }
        return true;
    }

    private StatusBarNotification getNotificationFromEvent(MotionEvent event) {
        for (int i = 0; i < mNotificationsContainer.getChildCount(); i++) {
            View view = mNotificationsContainer.getChildAt(i);
            Rect rect = new Rect(view.getLeft(),
                    view.getTop(), view.getRight(), view.getBottom());
            if (rect.contains((int) event.getX(), (int) event.getY())) {
                if (view.getTag() instanceof StatusBarNotification) {
                    return (StatusBarNotification) view.getTag();
                }
            }
        }
        return null;
    }

    private String getNotificationTickerText(StatusBarNotification n) {
        if (n == null) return "";

        String text = null;
        Notification notification = n.getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TITLE);
                if (notificationText != null) {
                    text = notificationText.toString();
                    return text;
                }
            }
        }

        if (notification.tickerText != null) {
            text = notification.tickerText.toString();
        }

        PackageManager pm = sContext.getPackageManager();
        if (text == null) {
            text = PanelHelper.getNotificationTitle(n);
            if (text == null) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(n.getPackageName(), 0);
                    text = (String) pm.getApplicationLabel(ai);
                } catch (PackageManager.NameNotFoundException e) {
                    // application is uninstalled, run away
                    text = "";
                }
            }
        }
        return text;
    }

    private Drawable getIconFromResource(StatusBarNotification n) {
        Drawable icon = null;
        String packageName = n.getPackageName();
        int resource = n.getNotification().icon;
        try {
            if (resource == 0) throw new PackageManager.NameNotFoundException("wrong resource");

            // notification icon
            Context remotePackageContext = sContext.createPackageContext(packageName, 0);
            icon = remotePackageContext.getResources().getDrawable(resource);

            // if icon is animated, get first frame
            if (icon instanceof AnimationDrawable) {
                icon = ((AnimationDrawable) icon).getFrame(0);
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            // this is bad
        } catch (Resources.NotFoundException rnfe) {
            // this is worse
        }

        if ((icon == null ||
                !(icon instanceof BitmapDrawable)) && n.getNotification().largeIcon != null) {
            // try to get large icon
            try {
                icon = new BitmapDrawable(sContext.getResources(), n.getNotification().largeIcon);
            } catch (Resources.NotFoundException rnfe) {
                // nope!
            }
        }

        if (icon instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
            int iconSize = sContext.getResources()
                    .getDimensionPixelSize(R.dimen.notification_icon_size);
            return new BitmapDrawable(sContext.getResources(),
                    Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true));
        } else {
            try {
                // last attempt, try to get app icon
                icon = sContext.getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // just in case we try to return a null icon at least we don't run into weird shit
                return sContext.getResources().getDrawable(R.drawable.ic_launcher);
            }
            return icon;
        }
    }

    public void unregisterEventReceivers() {
        mSensorHandler.unregisterScreenReceiver();
        mSensorHandler.unregisterEventListeners();
    }

    @Override
    public void onPocketModeChanged(boolean inPocket) {
        synchronized (this) {
            if (!inPocket && mNextNotification != null) {
                showNotification(mNextNotification, true);
                mNextNotification = null;
            }
        }
    }

    @Override
    public void onTableModeChanged(boolean onTable) {
        synchronized (this) {
            if (!onTable && mNextNotification != null) {
                showNotification(mNextNotification, true);
                mNextNotification = null;
            }
        }
    }

    @Override
    public void onScreenStateChaged(boolean screenOn) {
        if (!screenOn) {
            if (sScreenOnWakelock.isHeld()) {
                if (DEBUG) Log.d(TAG, "Screen is off. Releasing screen on wakelock");
                sScreenOnWakelock.release();
            }

            if (sPartialWakeLock.isHeld()) {
                if (DEBUG) Log.d(TAG, "Screen is off. Releasing partial wake lock");
                sPartialWakeLock.release();
            }
        } else {
            mNextNotification = null;
        }
        if (sBlockNextScreenOffIntent) {
            sBlockNextScreenOffIntent = false;
        } else {
            if (!screenOn && !PreferencesHelper
                    .usePowerOnNotifications(sContext)) {
                dismissNotification();
            }
        }
    }

    @Override
    public void onProximityValueReceived(float value) {
        // stub
    }

    @Override
    public void onGyroscopeValuesReceived(float x, float y, float z) {
        // stub
    }

    public static class PeekActivity extends Activity {

        private static final String PEEK_SHOWING_BROADCAST = "com.jedga.peek.PEEK_SHOWING";
        private static final String PEEK_HIDING_BROADCAST = "com.jedga.peek.PEEK_HIDING";

        private static final String TURN_SCREEN_ON = "screen_on";
        private static final String TURN_SCREEN_OFF = "screen_off";
        private static final String DISMISS_KEYGUARD = "dismiss_keyguard";
        private static final String FINISH_ACTIVITY = "finish";

        public static final String WAKE_UP = "wake_up";
        private static final int SCREEN_ON_DELAY = 500; //ms

        private ActivityReceiver mActivityReceiver;
        private Handler mHandler;
        private int mFlags;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            sBlockNextScreenOffIntent = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mFlags = WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            } else {
                mFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            }

            getWindow().addFlags(mFlags);

            mActivityReceiver = new ActivityReceiver();
            mHandler = new Handler();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(TURN_SCREEN_ON);
            filter.addAction(TURN_SCREEN_OFF);
            filter.addAction(DISMISS_KEYGUARD);
            filter.addAction(FINISH_ACTIVITY);
            registerReceiver(mActivityReceiver, filter);

            // update time and date
            sClockView.setText(getCurrentTimeText());
            sDateView.setText(getCurrentDateText());

            sPeekView.setAlpha(1f);
            sPeekView.setVisibility(View.VISIBLE);

            if (PreferencesHelper.useImmersiveMode(this) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sPeekView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }

            if (PreferencesHelper.useBlackBackground(this)) {
                sPeekView.setBackgroundColor(Color.BLACK);
            } else {
                Bitmap background = sWallpaperHelper.processBackground();
                if (background != null) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                    sPeekView.setBackground(
                            new BitmapDrawable(getResources(), background));
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
                    sPeekView.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            // broadcast all listening receivers that peek is showing
            Intent i = new Intent(PEEK_SHOWING_BROADCAST);
            sendBroadcast(i);

            // make sure sPeekView is not attached to any other container
            if (sPeekView != null) {
                ViewGroup parent = (ViewGroup) sPeekView.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
            }

            Intent startIntent = getIntent();
            if (startIntent.hasExtra(WAKE_UP)) {
                boolean wakeUp = startIntent.getBooleanExtra(WAKE_UP, false);
                if (wakeUp) turnScreenOn();
            }

            setContentView(sPeekView);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismissKeyguardAndNotification();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        protected void onResume() {
            scheduleTasks();
            super.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();

            overridePendingTransition(
                    sShouldAnimateTransition ? R.anim.slide_in : 0,
                    sShouldAnimateTransition ? R.anim.slide_out : 0);
        }

        @Override
        protected void onStop() {
            sHandler.removeCallbacksAndMessages(null);
            super.onStop();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mActivityReceiver);

            // broadcast all listening receivers that peek is hiding
            Intent i = new Intent(PEEK_HIDING_BROADCAST);
            sendBroadcast(i);
        }

        private void turnScreenOn() {
            if (!sScreenOnWakelock.isHeld()) {
                // Make sure activity has fully started by delaying
                // screen on wakelock by SCREEN_ON_DELAY
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sScreenOnWakelock.acquire();
                    }
                }, SCREEN_ON_DELAY);
            }
        }

        private void turnScreenOff() {
            DevicePolicyManager devicePolicyManager =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (devicePolicyManager.isAdminActive(
                    new ComponentName(PeekActivity.this, DeviceAdminHandler.class))) {
                devicePolicyManager.lockNow();
            }
        }

        private void dismissKeyguard() {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            if (sClickedItem != null) {
                sClickedItem.onKeyguardDismissed();
                sClickedItem = null; // oneshot
            }
        }

        private class ActivityReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(TURN_SCREEN_ON)) {
                    if (DEBUG) Log.d(TAG, "Turning screen on by request");
                    turnScreenOn();
                } else if (intent.getAction().equals(TURN_SCREEN_OFF)) {
                    if (DEBUG) Log.d(TAG, "Turning screen off by request");
                    turnScreenOff();
                } else if (intent.getAction().equals(DISMISS_KEYGUARD)) {
                    if (DEBUG) Log.d(TAG, "Dismissing keyguard by request");
                    dismissKeyguard();
                } else if (intent.getAction().equals(FINISH_ACTIVITY)) {
                    if (DEBUG) Log.d(TAG, "Finishing activity by request");
                    finish();
                } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    sClockView.setText(getCurrentTimeText());
                    sDateView.setText(getCurrentDateText());
                } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                    // unlocked device? dismiss notification, just in case
                    dismissNotification();
                }
            }
        }
    }

    public class NotificationClicker implements View.OnClickListener {

        private PendingIntent pendingIntent;
        private String packageName;

        public NotificationClicker(StatusBarNotification notification) {
            pendingIntent = notification.getNotification().contentIntent;
            packageName = notification.getPackageName();
        }

        public void onClick(View v) {
            if (pendingIntent != null) {
                sClickedItem = this;
                sShouldAnimateTransition = true;
                dismissKeyguardAndNotification();
            }
        }

        public void onKeyguardDismissed() {
            try {
                sHandler.removeCallbacksAndMessages(null); // avoid screen to turn off
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Intent startIntent = sContext
                        .getPackageManager().getLaunchIntentForPackage(packageName);
                sContext.startActivity(startIntent);
            }
        }
    }

    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mRingingOrConnected = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    mRingingOrConnected = false;
                    break;
            }
        }
    }
}