package com.jedga.peek.free.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LicenseVerifier {
    private static final String CALLBACK_INTENT = "com.jedga.peek.CALLBACK";
    private static final String VERIFY_INTENT = "com.jedga.peek.VERIFY";
    private static final String LICENSE_CHECK = "LICENSE_CHECK";

    private static final String ALLOWED = "LICENSE_ALLOWED";

    private static LicenseCallback sCallback;

    private static boolean sRegistered;

    private LicenseReceiver mReceiver;
    private Context mContext;

    public interface LicenseCallback {
        public abstract void onLicenseResponse(boolean allowed);
    }

    public LicenseVerifier(Context context) {
        mContext = context;
    }

    public void checkLicense(LicenseCallback callback) {
        sCallback = callback;

        mReceiver = new LicenseReceiver();
        IntentFilter filter = new IntentFilter(CALLBACK_INTENT);
        mContext.registerReceiver(mReceiver, filter);
        sRegistered = true;

        Intent verifyIntent = new Intent(VERIFY_INTENT);
        mContext.sendBroadcast(verifyIntent);
    }

    public void unregisterReceiver() {
        if (sRegistered) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException iae) {
                // already unregistered
            }
        }
    }

    public static class LicenseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context paramContext, Intent paramIntent) {
            if (paramIntent.getAction().equals(CALLBACK_INTENT)) {
                if (paramIntent.hasExtra(LICENSE_CHECK)) {
                    boolean allowed = paramIntent
                            .getStringExtra(LICENSE_CHECK).equals(ALLOWED);
                    sCallback.onLicenseResponse(allowed);
                    paramContext.unregisterReceiver(this);
                    sRegistered = false;
                }
            }
        }
    }
}
