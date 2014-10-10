/*
 * Copyright (C) 2014 ParanoidAndroid Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jedga.peek.free.helpers;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jedga.peek.free.R;

import java.util.ArrayList;
import java.util.List;

public class PanelHelper {

    public final static String DELIMITER = "|";
    private static final String FONT_FAMILY_LIGHT = "sans-serif-light";

    // Static methods
    public static void applyStyle(Context context, ViewGroup layout) {
        if (layout.getBackground() == null) {
            // if the notification has no background, use default notification background
            setViewBackground(context, layout);
        }

        List<View> subViews = getAllViewsInLayout(layout);
        for (int i = 0; i < subViews.size(); i++) {
            View v = subViews.get(i);
            if (v instanceof ViewGroup) { // change notification background
                v.setBackground(null);
            } else if (v instanceof ImageView) { // remove image background
                v.setBackground(null);
            } else if (v instanceof TextView) { // set font family
                boolean bold = v.getId() == android.R.id.title;
                TextView text = ((TextView) v);
                text.setTypeface(Typeface.create(FONT_FAMILY_LIGHT,
                        bold ? Typeface.BOLD : Typeface.NORMAL));
                text.setTextColor(Color.WHITE);
            }
        }
    }

    public static void setViewBackground(Context context, View v) {
        Drawable d = context.getResources().getDrawable(R.drawable.notification_background);
        v.setBackground(d);
    }

    private static List<View> getAllViewsInLayout(ViewGroup layout) {
        List<View> viewList = new ArrayList<View>();
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof ViewGroup) {
                viewList.addAll(getAllViewsInLayout((ViewGroup) v));
            }
            viewList.add(v);
        }
        return viewList;
    }

    public static boolean shouldDisplayNotification(
            StatusBarNotification oldNotif, StatusBarNotification newNotif) {
        // First check for ticker text, if they are different, some other parameters will be
        // checked to determine if we should show the notification.
        CharSequence oldTickerText = oldNotif.getNotification().tickerText;
        CharSequence newTickerText = newNotif.getNotification().tickerText;
        if (newTickerText == null ? oldTickerText == null : newTickerText.equals(oldTickerText)) {
            // If old notification title isn't null, show notification if
            // new notification title is different. If it is null, show notification
            // if the new one isn't.
            String oldNotificationText = getNotificationTitle(oldNotif);
            String newNotificationText = getNotificationTitle(newNotif);
            if (newNotificationText == null ? oldNotificationText != null :
                    !newNotificationText.equals(oldNotificationText)) return true;

            // Last chance, check when the notifications were posted. If times
            // are equal, we shouldn't display the new notification.
            if (oldNotif.getNotification().when != newNotif.getNotification().when) return true;
            return false;
        }
        return true;
    }

    public static String getNotificationTitle(StatusBarNotification n) {
        String text = null;
        if (n != null) {
            Notification notification = n.getNotification();
            Bundle extras = notification.extras;
            if (extras != null) {
                text = extras.getString(Notification.EXTRA_TITLE);
            }
        }
        return text;
    }

    public static String getContentDescription(StatusBarNotification content) {
        if (content != null) {
            return content.getPackageName() + DELIMITER + content.getId();
        }
        return null;
    }

    public static View.OnTouchListener getHighlightTouchListener() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setAlpha(0.5f);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setAlpha(1f);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Rect rect = new Rect();
                        view.getLocalVisibleRect(rect);
                        if (!rect.contains((int) event.getX(), (int) event.getY())) {
                            view.setAlpha(1f);
                        }
                        break;
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_CANCEL:
                        view.setAlpha(1f);
                        break;
                }
                return false;
            }
        };
    }
}