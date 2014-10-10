package com.jedga.peek.free;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceAdminHandler extends DeviceAdminReceiver {

    /**
     * Called when this application is approved to be a device administrator.
     */
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    /**
     * Called when this application is no longer the device administrator.
     */
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }
}
