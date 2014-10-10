package com.jedga.peek.free.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PackageHelper {

    public static List<AppContainer> getInstalledApps(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<AppContainer> apps = new ArrayList<AppContainer>();
        List<PackageInfo> list = pm.getInstalledPackages(0);

        for (PackageInfo pi : list) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pi.packageName, 0);
                if (ai == null) continue;
                if (ai.packageName.equals(context.getPackageName())) continue;
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && !PreferencesHelper.showSystemApps(context)) {
                    continue;
                }
                AppContainer app = new AppContainer();
                app.appPackage = ai.packageName;
                app.appName = ai.loadLabel(pm).toString();
                app.icon = ai.loadIcon(pm);
                apps.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(apps, new Comparator<AppContainer>() {
            public int compare(AppContainer app1, AppContainer app2) {
                return app1.appName.compareToIgnoreCase(app2.appName);
            }
        });

        return apps;
    }

    public static class AppContainer {
        public String appPackage;
        public String appName;
        public Drawable icon;
    }
}
