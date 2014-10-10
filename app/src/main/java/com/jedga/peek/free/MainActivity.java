package com.jedga.peek.free;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jedga.peek.free.helpers.ActivityHelpers;
import com.jedga.peek.free.helpers.PackageHelper;
import com.jedga.peek.free.helpers.PreferencesHelper;
import com.jedga.peek.free.helpers.SensorAdjustmentHelper;
import com.jedga.peek.free.layout.CustomGridViewAdapter;
import com.jedga.peek.free.layout.GestureDots;
import com.jedga.peek.free.preferences.SettingsFragment;
import com.jedga.peek.free.security.LicenseVerifier;

import fr.nicolaspomepuy.discreetapprate.AppRate;

public class MainActivity extends Activity {

    private static final String PRO_PACKAGE_NAME = "com.jedga.peek.pro";

    private static final int NOTIFICATION_ID = 0xF;
    private static final int LISTENER_STATE_CHANGE = 0x1;

    public static final String NATIVE_PEEK_STATE = "peek_state";
    private static final String NOTIFICATION_LISTENER_SETTINGS
            = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 0;
    private static boolean sIsOnMainFragment;
    private static Activity sActivity;
    private static Context sContext;
    private static DevicePolicyManager sDevicePolicyManager;
    private static LicenseVerifier sLicenseVerifier;
    private static FragmentManager sFragmentManager;
    private ContentResolver mContentResolver;
    private boolean mDialogShowing;

    private static Button mHintButton;

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(final boolean selfChange) {
            super.onChange(selfChange);
            if (isListenerServiceRunning()) {
                Intent thisActivity = new Intent(MainActivity.this, MainActivity.class);
                thisActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                thisActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(thisActivity, LISTENER_STATE_CHANGE);
            }
        }
    };

    private boolean mLicensed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        sActivity = this;
        sContext = getApplicationContext();
        sDevicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        sFragmentManager = getFragmentManager();
        sLicenseVerifier = new LicenseVerifier(sContext);

        if (savedInstanceState == null) {
            replaceFragment(false);

            AppRate.with(this)
                    .text(getString(R.string.app_rate))
                    .fromTop(true)
                    .checkAndShow();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (!isDeviceAdminEnabled()) {
            ComponentName deviceAdminComponentName
                    = new ComponentName(this, DeviceAdminHandler.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin));
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        }
    }

    @Override
    protected void onResume() {
        if (sActivity == null) sActivity = this;

        mLicensed = false;
        sLicenseVerifier.checkLicense(new LicenseVerifier.LicenseCallback() {
            @Override
            public void onLicenseResponse(boolean allowed) {
                mLicensed = allowed;
                invalidateOptionsMenu();
            }
        });

        if (!mDialogShowing) {
            if (!isListenerServiceRunning()) {
                mContentResolver = getContentResolver();
                Uri notificationListenerUri = Settings.Secure.getUriFor(
                        Settings.Secure.getString(
                                mContentResolver, "enabled_notification_listeners")
                );
                mContentResolver.registerContentObserver(
                        notificationListenerUri, true, mSettingsObserver);

                mDialogShowing = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(sActivity);
                builder.setMessage(R.string.notification_listener)
                        .setNegativeButton(R.string.go_to_settings,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent settingsIntent
                                                = new Intent(NOTIFICATION_LISTENER_SETTINGS);
                                        try {
                                            startActivity(settingsIntent);
                                        } catch (ActivityNotFoundException e) {
                                            // intent not found, notify user to search
                                            // for listener manually
                                            Toast.makeText(sContext, R.string.warning_no_intent,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        dialog.dismiss();
                                    }
                                }
                        )
                        .setPositiveButton(R.string.troubleshoot,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (sActivity == null) {
                                            sActivity = MainActivity.this;
                                        }

                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(sActivity);
                                        builder1.setMessage(R.string.troubleshoot_message);
                                        builder1.setCancelable(false);
                                        builder1.setNeutralButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        finish();
                                                    }
                                                }
                                        );
                                        builder1.show();
                                    }
                                }
                        );
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mDialogShowing = false;
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } else {
                if (PreferencesHelper.isFirstTimeRun(sContext)) {
                    mHintButton.performClick();
                }
            }

            // remove the notification
            NotificationManager nm
                    = (NotificationManager) sContext.getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // since the activity is static if we don't make it null on destroy it will leak
        sActivity = null;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(0); // go pro
        item.setVisible(!mLicensed && sIsOnMainFragment);
        item = menu.getItem(1); // settings
        item.setVisible(sIsOnMainFragment);
        item = menu.getItem(2); // help
        item.setVisible(sIsOnMainFragment);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                showSettings();
                break;
            case R.id.help:
                showHelp();
                break;
            case R.id.pro:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + PRO_PACKAGE_NAME)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id="
                                    + PRO_PACKAGE_NAME)
                    ));
                }
                break;
            case android.R.id.home:
                if (sFragmentManager.getBackStackEntryCount() > 0) {
                    sFragmentManager.popBackStackImmediate();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ENABLE_ADMIN:
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.device_admin_disabled, Toast.LENGTH_SHORT).show();
                }
                return;
            case LISTENER_STATE_CHANGE:
                mHintButton.performClick();
                mContentResolver.unregisterContentObserver(mSettingsObserver);
                return;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static void showSettings() {
        FragmentTransaction fragmentTransaction = sFragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.container, new SettingsFragment());

        if (!ActivityHelpers.isActivityFinished(sActivity)) {
            fragmentTransaction.commitAllowingStateLoss();
            sIsOnMainFragment = false;
        }
    }

    private void showHelp() {
        if (sActivity == null) sActivity = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(sActivity);
        builder.setTitle(R.string.help_title);
        builder.setMessage(R.string.help_message)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private static void replaceFragment(boolean animate) {
        boolean firstRun = PreferencesHelper.isFirstTimeRun(sContext);
        Fragment newFragment = firstRun && !animate ? new TutorialFragment() : new MainFragment();

        FragmentTransaction fragmentTransaction = sFragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(
                firstRun && animate ? R.anim.scale_from_bottom : 0,
                newFragment instanceof MainFragment ? R.anim.scale_to_top : 0);
        fragmentTransaction.replace(R.id.container, newFragment);

        if (!ActivityHelpers.isActivityFinished(sActivity)) {
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    private static void showTestNotification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(sActivity);
        builder.setMessage(R.string.test_message)
                .setTitle(R.string.test);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (sContext == null) sContext = sActivity.getApplicationContext();

                        Notification n = new Notification.Builder(sContext)
                                .setContentTitle(sContext.getString(R.string.notification_ticker))
                                .setContentText(sContext.getString(R.string.notification_text))
                                .setSmallIcon(R.drawable.notification_icon)
                                .build();

                        n.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
                        n.ledARGB = Color.WHITE;
                        n.ledOnMS = 500;
                        n.ledOffMS = 500;
                        n.flags |= Notification.FLAG_SHOW_LIGHTS;

                        NotificationManager nm
                                = (NotificationManager) sContext
                                .getSystemService(NOTIFICATION_SERVICE);
                        nm.notify(NOTIFICATION_ID, n);
                    }
                }, 2500);

                if (isDeviceAdminEnabled()) {
                    sDevicePolicyManager.lockNow();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isListenerServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();

        return !(enabledNotificationListeners == null
                || !enabledNotificationListeners.contains(packageName));

    }

    private static boolean isDeviceAdminEnabled() {
        ComponentName deviceAdminComponentName
                = new ComponentName(sContext, DeviceAdminHandler.class);
        return sDevicePolicyManager.isAdminActive(deviceAdminComponentName);
    }

    public static class TutorialFragment extends Fragment {
        private static final int HINT_DELAY = 2500;

        private ViewGroup mMainFrame;
        private ViewGroup mRootView;

        private TextView mWelcomeText;
        private ImageView mHintRing;
        private TextView mHintText;
        private GestureDots mRightDots;
        private GestureDots mLeftDots;
        private View mDivider;

        private Handler mHandler;

        public TutorialFragment() {
        }

        public static float measureScale() {
            WindowManager wm = (WindowManager) sContext.getSystemService(WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);

            float displayHeight = outMetrics.heightPixels;
            float displayWidth = outMetrics.widthPixels;
            return Math.max(displayHeight, displayWidth);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View fragmentView = inflater.inflate(R.layout.tutorial_main, container, false);
            mMainFrame = (ViewGroup) fragmentView.findViewById(R.id.main_frame);
            mRootView = (ViewGroup) mMainFrame.findViewById(R.id.container);

            mWelcomeText = (TextView) mRootView.findViewById(R.id.welcome_text);
            mHintRing = (ImageView) mRootView.findViewById(R.id.hint_ring);
            mHintText = (TextView) mRootView.findViewById(R.id.hint_text);
            mDivider = mRootView.findViewById(R.id.divider);

            int backgroundSize = getResources()
                    .getDimensionPixelSize(R.dimen.notification_icon_background_size);

            mRightDots = new GestureDots(getActivity(), 4);
            mRightDots.createDots(mRootView, backgroundSize, 1, true);
            mRightDots.setActionResource(R.drawable.ic_dismiss);

            mLeftDots = new GestureDots(getActivity(), 4);
            mLeftDots.createDots(mRootView, backgroundSize, 6, false);
            mLeftDots.setActionResource(R.drawable.ic_unlock);

            mHintButton = (Button) mRootView.findViewById(R.id.hint_button);
            mHintButton.setText(R.string.get_started);
            mHintButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    view.animate().translationYBy(view.getHeight())
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mHintText.animate().alpha(1f);
                                    showFirstHint();
                                    super.onAnimationEnd(animation);
                                }
                            });
                }
            });

            mHandler = new Handler();
            return fragmentView;
        }

        private void showFirstHint() {
            mWelcomeText.setText(R.string.demo);
            mHintRing.setVisibility(View.VISIBLE);
            mHintText.setVisibility(View.VISIBLE);
            mHintText.setText(R.string.hold_to_expand_hint);

            mHintButton.setText(R.string.skip);
            mHintButton.setVisibility(View.VISIBLE);
            mHintButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            replaceFragment(true);
                        }
                    }
            );

            TextView expandedText = (TextView) mRootView.findViewById(R.id.hint_summary);
            expandedText.animate().alpha(1f);
            mHintRing.animate().scaleX(1.2f).scaleY(1.2f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mHintRing.animate().scaleX(1f)
                                    .scaleY(1f).setStartDelay(HINT_DELAY)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            mHintRing.animate().setStartDelay(0);
                                            showSecondHint();
                                            super.onAnimationEnd(animation);
                                        }
                                    });
                            super.onAnimationEnd(animation);
                        }
                    });
        }

        private void showSecondHint() {
            crossfadeText(R.string.swipe_right_hint);
            mRightDots.animateIn();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showThirdHint();
                }
            }, HINT_DELAY);
        }

        private void showThirdHint() {
            crossfadeText(R.string.swipe_left_hint);
            mRightDots.animateOut();
            mLeftDots.animateIn();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showFourthHint();
                }
            }, HINT_DELAY);
        }

        private void showFourthHint() {
            crossfadeText(R.string.swipe_up_hint);
            mRightDots.animateIn();
            mHintRing.animate().translationY(-50)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    showFifthHint();
                                }
                            }, HINT_DELAY);
                            super.onAnimationEnd(animation);
                        }
                    });
        }

        private void showFifthHint() {
            crossfadeText(R.string.swipe_down_hint);
            mHintRing.animate().translationY(50)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mHintRing.animate().translationY(0).setStartDelay(HINT_DELAY)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            mHintRing.animate().setStartDelay(0);
                                            showLastHint();
                                            super.onAnimationEnd(animation);
                                        }
                                    });
                            super.onAnimationEnd(animation);
                        }
                    });
        }



        private void showLastHint() {
            mDivider.setVisibility(View.VISIBLE);
            mWelcomeText.setVisibility(View.GONE);
            crossfadeText(R.string.empty);
            mRightDots.animateOut();
            mLeftDots.animateOut();

            // da animation!
            View animatorView = mMainFrame.findViewById(R.id.animator_view);
            float scale = measureScale();
            animatorView.setVisibility(View.VISIBLE);
            animatorView.animate().scaleX(scale).scaleY(scale);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mHintButton.animate().translationY(0).setListener(null);
                    mHintButton.setText(R.string.understood);
                    mHintText.animate().alpha(0f);
                }
            });
        }

        private void crossfadeText(final int newText) {
            mHintText.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mHintText.setText(newText);
                    mHintText.animate().alpha(1f).setListener(null);
                    super.onAnimationEnd(animation);
                }
            });
        }
    }

    public static class MainFragment extends Fragment {

        private static final int ANIM_DURATION = 600;

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Window w = getActivity().getWindow();
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // restore translucent flags
                w.addFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                );
            }

            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(false);

            View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);

            final TextView switchText = (TextView) fragmentView.findViewById(R.id.switch_text);
            final Button switchButton = (Button) fragmentView.findViewById(R.id.switch_button);
            switchButton.setScaleX(0);
            switchButton.setScaleY(0);
            switchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setPeekStatus(switchButton, switchText, true);
                }
            });

            setPeekStatus(switchButton, switchText, false);

            Button testButton = (Button) fragmentView.findViewById(R.id.test_button);
            testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    if (sActivity == null) sActivity = getActivity();
                    showTestNotification();
                }
            });

            GridView appDrawerContainer
                    = (GridView) fragmentView.findViewById(R.id.app_drawer_container);
            CustomGridViewAdapter adapter = new CustomGridViewAdapter(sContext,
                    R.layout.app_drawer_item, PackageHelper.getInstalledApps(sContext));
            appDrawerContainer.setAdapter(adapter);

            switchButton.animate()
                    .scaleY(1f).scaleX(1f).setDuration(ANIM_DURATION);

            // show settings button
            sIsOnMainFragment = true;
            getActivity().invalidateOptionsMenu();

            // if the ROM has native peek, disable it
            try {
                ContentResolver cr = sContext.getContentResolver();
                boolean nativePeekEnabled = Settings.System.getInt(cr, NATIVE_PEEK_STATE) == 1;
                if (nativePeekEnabled) {
                    Settings.System.putInt(cr, NATIVE_PEEK_STATE, 0);
                    Toast.makeText(sContext,
                            R.string.native_peek_disabled, Toast.LENGTH_LONG).show();
                }
            } catch (Settings.SettingNotFoundException e) {
                // ROM doesn't have native peek
            }

            if (PreferencesHelper.isFirstTimeRun(sContext)) {
                SensorAdjustmentHelper helper = new SensorAdjustmentHelper(getActivity(), true);
                helper.startAdjustment();
            }

            PreferencesHelper.setFirstTimeRun(getActivity(), false);
            return fragmentView;
        }

        private void setPeekStatus(View switchButton, TextView switchText, boolean flip) {
            boolean isEnabled = PreferencesHelper.isPeekEnabled(sContext);
            if (flip) isEnabled = !isEnabled;
            switchButton.animate().rotationYBy(180).setDuration(500); // naise!
            switchButton.setBackground(getResources().getDrawable(isEnabled ?
                    R.drawable.switch_background_on :
                    R.drawable.switch_background_off));
            switchText.setText(getResources().getString(
                    isEnabled ? R.string.peek_enabled : R.string.peek_disabled));
            if (flip) {
                PreferencesHelper.setPeekStatus(sContext, isEnabled);
            }
        }
    }
}
