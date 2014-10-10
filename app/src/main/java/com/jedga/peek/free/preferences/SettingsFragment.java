package com.jedga.peek.free.preferences;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.Window;
import android.view.WindowManager;

import com.jedga.peek.free.R;
import com.jedga.peek.free.helpers.PreferencesHelper;
import com.jedga.peek.free.helpers.SensorAdjustmentHelper;
import com.jedga.peek.free.layout.CustomListPreference;
import com.jedga.peek.free.security.LicenseVerifier;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String SENSOR_ADJUSTMENT = "sensor_adjustment";

    // customization
    private CheckBoxPreference mPowerOnNotifications;
    private CheckBoxPreference mImmersiveMode;
    private CheckBoxPreference mBackground;
    private CheckBoxPreference mReverseGestures;
    private CheckBoxPreference mSystemApps;

    // sensors
    private CheckBoxPreference mWakeUp;
    private CustomListPreference mDetectionType;
    private EditTextPreference mSensorPollingTime;
    private Preference mSensorAdjustment;

    // security
    private CheckBoxPreference mShowNotificationTitle;
    private CheckBoxPreference mShowNotificationContent;

    // partners
    private Preference mXionidis;
    private Preference mArz;
    private Preference mHuot;
    private Preference mTaylor;

    private KeyguardManager mKeyguardManager;

    private LicenseVerifier mLicenseVerifier;
    private boolean mLicensed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getActivity().getWindow();
        w.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        );
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        getActivity().invalidateOptionsMenu();

        mLicenseVerifier = new LicenseVerifier(getActivity());

        addPreferencesFromResource(R.xml.settings);

        mKeyguardManager
                = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);

        PreferenceCategory customization
                = (PreferenceCategory) findPreference(PreferencesHelper.CUSTOMIZATION_CATEGORY);

        mPowerOnNotifications = (CheckBoxPreference) findPreference(PreferencesHelper.POWER_ON);
        if (mPowerOnNotifications != null) {
            mPowerOnNotifications.setOnPreferenceChangeListener(this);
            if (mPowerOnNotifications.isChecked()) {
                mPowerOnNotifications
                        .setSummary(getString(R.string.power_on_notifications_checked));
            } else {
                mPowerOnNotifications
                        .setSummary(getString(R.string.power_on_notifications_unchecked));
            }
        }

        mImmersiveMode = (CheckBoxPreference) findPreference(PreferencesHelper.IMMERSIVE_MODE);
        if (mImmersiveMode != null) {
            mImmersiveMode.setOnPreferenceChangeListener(this);
            if (mImmersiveMode.isChecked()) {
                mImmersiveMode.setSummary(getString(R.string.immersive_mode_checked));
            } else {
                mImmersiveMode.setSummary(getString(R.string.immersive_mode_unchecked));
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                customization.removePreference(mImmersiveMode);
            }
        }

        mBackground = (CheckBoxPreference) findPreference(PreferencesHelper.BACKGROUND);
        if (mBackground != null) {
            mBackground.setOnPreferenceChangeListener(this);
            if (mBackground.isChecked()) {
                mBackground.setSummary(getString(R.string.background_checked));
            } else {
                mBackground.setSummary(getString(R.string.background_unchecked));
            }
        }

        mReverseGestures = (CheckBoxPreference) findPreference(PreferencesHelper.REVERSE_GESTURES);
        if (mReverseGestures != null) {
            mReverseGestures.setOnPreferenceChangeListener(this);
            if (mReverseGestures.isChecked()) {
                mReverseGestures.setSummary(getString(R.string.reverse_gestures_checked));
            } else {
                mReverseGestures.setSummary(getString(R.string.reverse_gestures_unchecked));
            }
        }

        mSystemApps = (CheckBoxPreference) findPreference(PreferencesHelper.SYSTEM_APPS);
        if (mSystemApps != null) {
            mSystemApps.setOnPreferenceChangeListener(this);
            if (mSystemApps.isChecked()) {
                mSystemApps.setSummary(getString(R.string.system_apps_show));
            } else {
                mSystemApps.setSummary(getString(R.string.system_apps_hide));
            }
        }

        mWakeUp = (CheckBoxPreference) findPreference(PreferencesHelper.WAKE_UP);
        if (mWakeUp != null) {
            mWakeUp.setOnPreferenceChangeListener(this);
            if (mWakeUp.isChecked()) {
                mWakeUp.setSummary(getString(R.string.wake_up_checked));
            } else {
                mWakeUp.setSummary(getString(R.string.wake_up_unchecked));
            }
        }

        mDetectionType = (CustomListPreference) findPreference(PreferencesHelper.DETECTION_TYPE);
        if (mDetectionType != null) {
            mDetectionType.setOnPreferenceChangeListener(this);
        }

        mSensorPollingTime = (EditTextPreference) findPreference(PreferencesHelper.POLLING_TIME);

        mSensorAdjustment = findPreference(SENSOR_ADJUSTMENT);
        mSensorAdjustment.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SensorAdjustmentHelper helper = new SensorAdjustmentHelper(getActivity(), false);
                helper.startAdjustment();
                return false;
            }
        });

        mShowNotificationTitle
                = (CheckBoxPreference) findPreference(PreferencesHelper.SHOW_NOTIFICATION_TITLE);
        if (mShowNotificationTitle != null) {
            mShowNotificationTitle.setOnPreferenceChangeListener(this);
            if (mShowNotificationTitle.isChecked()) {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_checked));
            } else {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_unchecked));
            }
        }

        mShowNotificationContent
                = (CheckBoxPreference) findPreference(PreferencesHelper.SHOW_NOTIFICATION_CONTENT);
        if (mShowNotificationContent != null) {
            mShowNotificationContent.setOnPreferenceChangeListener(this);
            if (mShowNotificationContent.isChecked()) {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_checked));
            } else {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_unchecked));
            }
        }

        mXionidis = findPreference(PreferencesHelper.XIONIDIS);
        mXionidis.setOnPreferenceClickListener(this);

        mArz = findPreference(PreferencesHelper.ARZ);
        mArz.setOnPreferenceClickListener(this);

        mHuot = findPreference(PreferencesHelper.HUOT);
        mHuot.setOnPreferenceClickListener(this);

        mTaylor = findPreference(PreferencesHelper.TAYLOR);
        mTaylor.setOnPreferenceClickListener(this);

        updatePreferences(null);
    }

    @Override
    public void onResume() {
        mLicensed = false;
        mLicenseVerifier.checkLicense(new LicenseVerifier.LicenseCallback() {
            @Override
            public void onLicenseResponse(boolean allowed) {
                mLicensed = allowed;
                updateAvailableSettings();
            }
        });
        updateAvailableSettings();

        boolean secureLock = mKeyguardManager.isKeyguardSecure();
        mShowNotificationTitle.setEnabled(secureLock);
        mShowNotificationContent.setEnabled(secureLock);
        if (secureLock) {
            // title
            if (mShowNotificationTitle.isChecked()) {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_checked));
            } else {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_unchecked));
            }

            // content
            if (mShowNotificationContent.isChecked()) {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_checked));
            } else {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_unchecked));
            }
        } else {
            mShowNotificationTitle.setSummary(R.string.security_disabled);
            mShowNotificationContent.setSummary(R.string.security_disabled);
        }
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String key = preference.getKey();

        if (key == null) return false;

        if (key.equals(PreferencesHelper.POWER_ON)) {
            if ((Boolean) o) {
                mPowerOnNotifications
                        .setSummary(getString(R.string.power_on_notifications_checked));
            } else {
                mPowerOnNotifications
                        .setSummary(getString(R.string.power_on_notifications_unchecked));
            }
        } else if (key.equals(PreferencesHelper.IMMERSIVE_MODE)) {
            if ((Boolean) o) mImmersiveMode.setSummary(getString(R.string.immersive_mode_checked));
            else mImmersiveMode.setSummary(getString(R.string.immersive_mode_unchecked));
        } else if (key.equals(PreferencesHelper.BACKGROUND)) {
            if ((Boolean) o) mBackground.setSummary(getString(R.string.background_checked));
            else mBackground.setSummary(getString(R.string.background_unchecked));
        } else if (key.equals(PreferencesHelper.REVERSE_GESTURES)) {
            if ((Boolean) o) {
                mReverseGestures.setSummary(getString(R.string.reverse_gestures_checked));
            } else {
                mReverseGestures.setSummary(getString(R.string.reverse_gestures_unchecked));
            }
        } else if (key.equals(PreferencesHelper.SYSTEM_APPS)) {
            if ((Boolean) o) {
                mSystemApps.setSummary(getString(R.string.system_apps_show));
            } else {
                mSystemApps.setSummary(getString(R.string.system_apps_hide));
            }
        } else if (key.equals(PreferencesHelper.WAKE_UP)) {
            if ((Boolean) o) {
                mWakeUp.setSummary(getString(R.string.wake_up_checked));
            } else {
                mWakeUp.setSummary(getString(R.string.wake_up_unchecked));
            }
        } else if (key.equals(PreferencesHelper.DETECTION_TYPE)) {
            updatePreferences((String) o);
        } else if (key.equals(PreferencesHelper.SHOW_NOTIFICATION_TITLE)) {
            if ((Boolean) o) {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_checked));
            } else {
                mShowNotificationTitle
                        .setSummary(getString(R.string.show_notification_title_unchecked));
            }
        } else if (key.equals(PreferencesHelper.SHOW_NOTIFICATION_CONTENT)) {
            if ((Boolean) o) {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_checked));
            } else {
                mShowNotificationContent
                        .setSummary(getString(R.string.show_notification_content_unchecked));
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key == null) return false;

        if (key.equals(PreferencesHelper.XIONIDIS)) {
            startUrlActivity(PreferencesHelper.XIONIDIS_URL);
        } else if (key.equals(PreferencesHelper.ARZ)) {
            startUrlActivity(PreferencesHelper.ARZ_URL);
        } else if (key.equals(PreferencesHelper.HUOT)) {
            startUrlActivity(PreferencesHelper.HUOT_URL);
        } else if (key.equals(PreferencesHelper.TAYLOR)) {
            startUrlActivity(PreferencesHelper.TAYLOR_URL);
        }

        return true;
    }

    private void updateAvailableSettings() {
        mPowerOnNotifications.setEnabled(mLicensed);
        mReverseGestures.setEnabled(mLicensed);
        mWakeUp.setEnabled(mLicensed);
        mDetectionType.setEnabled(mLicensed);
        mSensorPollingTime.setEnabled(mLicensed);
        updatePreferenceSummaries();
        if (!mLicensed) {
            mPowerOnNotifications.setSummary(R.string.only_available_on_pro);
            mReverseGestures.setSummary(R.string.only_available_on_pro);
            mWakeUp.setSummary(R.string.only_available_on_pro);
            mDetectionType.setSummary(R.string.only_available_on_pro);
            mSensorPollingTime.setSummary(R.string.only_available_on_pro);
        }
    }

    private void updatePreferenceSummaries() {
        mPowerOnNotifications.setSummary(mPowerOnNotifications.isChecked() ?
                R.string.power_on_notifications_checked : R.string.power_on_notifications_unchecked);
        mReverseGestures.setSummary(mReverseGestures.isChecked() ?
                R.string.reverse_gestures_checked : R.string.reverse_gestures_unchecked);
        mWakeUp.setSummary(mWakeUp.isChecked() ?
                R.string.wake_up_checked : R.string.wake_up_unchecked);
        String[] summaryArrays = getResources().getStringArray(R.array.detection_type_entries);
        int index = Integer.parseInt(mDetectionType.getValue());
        mDetectionType.setSummary(summaryArrays[index]);
        mSensorPollingTime.setSummary(R.string.sensor_polling_time_summary);
    }

    private void updatePreferences(String value) {
        if (value == null) {
            value = mDetectionType.getValue();
        } else {
            if (Integer.parseInt(value) == PreferencesHelper.TYPE_ALWAYS_ACTIVE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.detection_type_warning_message)
                        .setTitle(R.string.detection_type_warning);
                builder.setNeutralButton(android.R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        String[] summaryArrays = getResources().getStringArray(R.array.detection_type_entries);
        int index = Integer.parseInt(value);
        mDetectionType.setSummary(summaryArrays[index]);
        mSensorPollingTime.setEnabled(index != PreferencesHelper.TYPE_ALWAYS_ACTIVE);
    }

    private void startUrlActivity(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
        }
    }
}
