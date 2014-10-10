package com.jedga.peek.free.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;

public class ActivityHelpers {
    public static boolean isActivityFinished(Activity activity) {
        return isActivityFinishedJb(activity);
    }


    private static boolean isActivityFinishedNormal(Activity activity) {
        return activity == null ||
                activity.isFinishing();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static boolean isActivityFinishedHc(Activity activity) {
        return isActivityFinishedNormal(activity) ||
                activity.isChangingConfigurations();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static boolean isActivityFinishedJb(Activity activity) {
        return isActivityFinishedHc(activity) ||
                activity.isDestroyed();
    }


    public static boolean isFragmentFinished(Fragment fragment) {
        return fragment == null ||
                fragment.isDetached() ||
                fragment.isRemoving() ||
                isActivityFinished(fragment.getActivity());
    }
}
