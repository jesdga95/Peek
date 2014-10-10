package com.jedga.peek.free.layout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.jedga.peek.free.R;

public class GestureDots {

    private static final int ITEM_SPACING = 45;
    private static final int ANIM_DELAY = 80; // ms
    private static final int ANIM_DELAY_FRACTION = 20; // ms
    private static final float ALPHA_NO_HIGHLIGHT = 0.1f;
    private static final float ALPHA_HIGHLIGHT = 1f;
    private ImageView[] mDots;
    private ImageView mAction;
    private Context mContext;
    private boolean mShowing;

    public GestureDots(Context context, int dots) {
        mContext = context;
        mDots = new ImageView[dots];
        mAction = new ImageView(context);
    }

    public void createDots(
            ViewGroup parent, int paddingOffset, int idMultiplier, boolean right) {
        for (int i = 1; i <= mDots.length; i++) {
            int index = i - 1;
            mDots[index] = new ImageView(mContext);
            mDots[index].setAlpha(0f);
            mDots[index].setImageResource(R.drawable.dot_shape);

            RelativeLayout.LayoutParams dotLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);

            int dotId = idMultiplier * i * 10;
            mDots[index].setId(dotId);

            if (i == 1) {
                mDots[index].setPadding(
                        right ? Math.round(paddingOffset * 1.1f) : 0, 0,
                        right ? 0 : Math.round(paddingOffset * 1.1f), 0);
                parent.addView(mDots[index]);
                dotLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                mDots[index].setLayoutParams(dotLayoutParams);
            } else {
                mDots[index].setPadding(
                        right ? ITEM_SPACING : 0, 0, right ? 0 : ITEM_SPACING, 0);
                parent.addView(mDots[index]);
                dotLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                dotLayoutParams.addRule(
                        right ? RelativeLayout.RIGHT_OF :
                                RelativeLayout.LEFT_OF, idMultiplier * (i - 1) * 10
                );
                mDots[index].setLayoutParams(dotLayoutParams);
            }
        }

        RelativeLayout.LayoutParams actionLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        mAction.setPadding(
                right ? ITEM_SPACING : 0, 0, right ? 0 : ITEM_SPACING, 0);
        mAction.setScaleX(0f);
        mAction.setScaleY(0f);
        actionLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        actionLayoutParams.addRule(
                right ? RelativeLayout.RIGHT_OF :
                        RelativeLayout.LEFT_OF, idMultiplier * 40
        );
        mAction.setLayoutParams(actionLayoutParams);
        parent.addView(mAction);
    }

    public void setActionResource(int resource) {
        mAction.setImageResource(resource);
    }

    public void animateIn() {
        if (mShowing) return;
        mShowing = true;
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < mDots.length; i++) {
            final int index = i;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDots[index].animate().alpha(ALPHA_NO_HIGHLIGHT);
                    if (index == (mDots.length - 1)) startAnimationHint();
                }
            }, i * ANIM_DELAY);
        }
        mAction.animate().scaleX(1f).scaleY(1f);
    }

    public void animateOut() {
        if (!mShowing) return;
        mShowing = false;
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = mDots.length - 1; i >= 0; i--) {
            final int index = i;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDots[index].animate().alpha(0f);
                }
            }, i * ANIM_DELAY);
        }
        mAction.animate().scaleX(0f).scaleY(0f);
    }

    public void startAnimationHint() {
        final Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mShowing) {
                    for (int i = 0; i <= mDots.length; i++) {
                        if (!mShowing) break;
                        final int index = i;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (index < mDots.length) {
                                    mDots[index].animate().alpha(ALPHA_HIGHLIGHT);
                                }
                                if (index - 1 >= 0) mDots[index - 1]
                                        .animate().alpha(ALPHA_NO_HIGHLIGHT);
                            }
                        }, i * ANIM_DELAY_FRACTION);
                        try {
                            Thread.sleep(mDots.length * ANIM_DELAY_FRACTION
                                    * (index == mDots.length ? 10 : 1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                animateOut(); // make sure we didn't left dots
                handler.removeCallbacksAndMessages(null);
            }
        });
        thread.start();
    }
}
