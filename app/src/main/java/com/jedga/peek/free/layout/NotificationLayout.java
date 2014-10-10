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

package com.jedga.peek.free.layout;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.jedga.peek.free.NotificationPeek;
import com.jedga.peek.free.helpers.PreferencesHelper;
import com.jedga.peek.free.helpers.SwipeHelper;
import com.jedga.peek.free.service.ListenerService;

public class NotificationLayout extends LinearLayout {

    private SwipeHelper mSwipeHelperX;
    private SwipeHelper mSwipeHelperY;
    private boolean mTouched;

    private NotificationPeek mBridge;

    private Context mContext;

    public NotificationLayout(Context context) {
        this(context, null);
    }

    public NotificationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        float densityScale = context.getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        SwipeHelperX helperX = new SwipeHelperX();
        mSwipeHelperX = new SwipeHelper(SwipeHelper.X, helperX, densityScale, pagingTouchSlop);
        mSwipeHelperX.setAllowDragAfterLongPress(true);
        mSwipeHelperX.setLongPressListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });

        SwipeHelperY helperY = new SwipeHelperY();
        mSwipeHelperY = new SwipeHelper(SwipeHelper.Y, helperY, densityScale, pagingTouchSlop);
        mSwipeHelperY.setAllowDragAfterLongPress(true);
        mSwipeHelperY.setLongPressListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });
    }

    public void setViewBridge(NotificationPeek bridge) {
        mBridge = bridge;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getContext().getResources().getDisplayMetrics().density;
        mSwipeHelperX.setDensityScale(densityScale);
        mSwipeHelperY.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelperX.setPagingTouchSlop(pagingTouchSlop);
        mSwipeHelperY.setPagingTouchSlop(pagingTouchSlop);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            StatusBarNotification n
                    = (StatusBarNotification) mBridge.getNotificationView().getTag();
            if (n != null) {
                boolean reverseGestures = PreferencesHelper.useReversedGestures(mContext);
                if (reverseGestures) {
                    mBridge.animateDots(true, true, n.isClearable());
                } else {
                    mBridge.animateDots(true, n.isClearable(), true);
                }
            }
            mBridge.setAnimating(true);
            mTouched = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mBridge.animateDots(false, true, true);
            mBridge.setAnimating(false);
            mTouched = false;
        }
        return mSwipeHelperX.onInterceptTouchEvent(event) ||
                mSwipeHelperY.onInterceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mBridge.animateDots(false, true, true);
            mBridge.setAnimating(false);
            mTouched = false;
        }
        return mSwipeHelperX.onTouchEvent(event) ||
                mSwipeHelperY.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class SwipeHelperX implements SwipeHelper.Callback {

        @Override
        public View getChildAtPosition(MotionEvent ev) {
            return getChildContentView(null);
        }

        @Override
        public View getChildContentView(View v) {
            return mBridge.getNotificationView();
        }

        @Override
        public boolean isConstrainSwipeEnabled() {
            return true;
        }

        @Override
        public boolean isFadeoutEnabled(int gestureDirection) {
            boolean reverseGestures = PreferencesHelper.useReversedGestures(mContext);
            return gestureDirection ==
                    (reverseGestures ? SwipeHelper.GESTURE_NEGATIVE : SwipeHelper.GESTURE_POSITIVE);
        }

        @Override
        public boolean canChildBeDismissed(int gestureDirection, View v) {
            boolean reverseGestures = PreferencesHelper.useReversedGestures(mContext);
            StatusBarNotification n = (StatusBarNotification) mBridge.getNotificationView().getTag();
            return gestureDirection == (reverseGestures
                    ? SwipeHelper.GESTURE_POSITIVE : SwipeHelper.GESTURE_NEGATIVE) ||
                    gestureDirection == (reverseGestures
                            ? SwipeHelper.GESTURE_NEGATIVE
                            : SwipeHelper.GESTURE_POSITIVE) && n.isClearable();
        }

        @Override
        public void onChildDismissed(int gestureDirection, View v) {
            boolean reverseGestures = PreferencesHelper.useReversedGestures(mContext);
            if (gestureDirection == (reverseGestures ?
                    SwipeHelper.GESTURE_NEGATIVE : SwipeHelper.GESTURE_POSITIVE)) {
                StatusBarNotification n = (StatusBarNotification)
                        mBridge.getNotificationView().getTag();
                Intent i = new Intent(ListenerService.REMOVE_NOTIFICATION_FROM_STATUSBAR);
                i.putExtra("package", n.getPackageName());
                i.putExtra("tag", n.getTag());
                i.putExtra("id", n.getId());
                mContext.sendBroadcast(i);
            } else {
                mBridge.dismissKeyguardAndNotification();
            }
        }

        @Override
        public void onBeginDrag(View v) {
        }

        @Override
        public void onDragUpdate(int gestureDirection, float progress) {
        }

        @Override
        public void onDragCancelled(View v) {
            mBridge.animateDots(false, true, true);
        }
    }

    private class SwipeHelperY implements SwipeHelper.Callback {

        boolean isDragging;
        boolean hasExpanded;
        boolean hasOpened;

        @Override
        public View getChildAtPosition(MotionEvent ev) {
            return getChildContentView(null);
        }

        @Override
        public View getChildContentView(View v) {
            return mBridge.getNotificationView();
        }

        @Override
        public boolean isConstrainSwipeEnabled() {
            return true;
        }

        @Override
        public boolean isFadeoutEnabled(int gestureDirection) {
            return false;
        }

        @Override
        public boolean canChildBeDismissed(int gestureDirection, View v) {
            return false;
        }

        @Override
        public void onChildDismissed(int gestureDirection, View v) {

        }

        @Override
        public void onBeginDrag(View v) {
        }

        @Override
        public void onDragUpdate(int gestureDirection, float progress) {
            if(gestureDirection == SwipeHelper.GESTURE_NEGATIVE
                    && progress == SwipeHelper.TOP_DISTANCE_CONSTRAINT_DISABLED) {
                if(!hasExpanded) {
                    mBridge.toggleNotificationExpanded(true);
                    hasExpanded = true;
                }
            } else if(gestureDirection == SwipeHelper.GESTURE_POSITIVE) {
                if(progress == SwipeHelper.TOP_DISTANCE_CONSTRAINT_DISABLED) {
                    if (!hasOpened) {
                        hasOpened = true;
                    }
                }
            }

            if(progress != 0 && !isDragging) {
                mBridge.animateDots(false, true, true);
                mBridge.toggleBottomIcon(true);
                isDragging = true;
            }

            if(mTouched && hasOpened && progress != SwipeHelper.TOP_DISTANCE_CONSTRAINT_DISABLED) {
                hasOpened = false;
            }
        }

        @Override
        public void onDragCancelled(View v) {
            mBridge.toggleBottomIcon(false);
            hasExpanded = false;
            if (!mTouched && hasOpened) {
                mBridge.openNotificationIntent();
                hasOpened = false;
            }
            isDragging = false;
        }
    }
}