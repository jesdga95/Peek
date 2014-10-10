package com.jedga.peek.free.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.Toast;

import com.jedga.peek.free.R;
import com.jedga.peek.free.layout.ImageDialog;

public class SensorAdjustmentHelper implements SensorActivityHandler.SensorChangedCallback {

    private SensorActivityHandler mSensorHandler;
    private Activity mActivity;
    private boolean mFirstRun;

    // flags
    private boolean mStoreTableValues;
    private boolean mStoreLiftingValue;
    private boolean mStoreProximityValue;

    // values to store
    private float[] mTableValues;
    private float[] mLiftValues;
    private float mProximityValue;

    public SensorAdjustmentHelper(Activity activity, boolean firstRun) {
        mActivity = activity;
        mFirstRun = firstRun;
    }

    public void startAdjustment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setCancelable(false);
        builder.setMessage(mFirstRun
                ? R.string.sensor_adjustment_start_first_run : R.string.sensor_adjustment_start);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                mSensorHandler
                        = new SensorActivityHandler(mActivity, SensorAdjustmentHelper.this, true);
                startTableAdjustment();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void startTableAdjustment() {
        mSensorHandler.registerGyroscopeEvent();
        ImageDialog.showDialog(mActivity,
                mActivity.getString(R.string.sensor_adjustment_table),
                mActivity.getString(R.string.sensor_adjustment_table_text),
                mActivity.getString(R.string.sensor_adjustment_next),
                mActivity.getResources().getDrawable(R.drawable.place_table),
                new ImageDialog.ClickCallback() {
                    @Override
                    public void onClick() {
                        startLiftingAdjustment();
                        mStoreTableValues = true;
                    }
                }
        );
    }

    public void startLiftingAdjustment() {
        ImageDialog.showDialog(mActivity,
                mActivity.getString(R.string.sensor_adjustment_table),
                mActivity.getString(R.string.sensor_adjustment_lift_text),
                mActivity.getString(R.string.sensor_adjustment_next),
                mActivity.getResources().getDrawable(R.drawable.lift_table),
                new ImageDialog.ClickCallback() {
                    @Override
                    public void onClick() {
                        startProximityAdjustment();
                        mStoreLiftingValue = true;
                    }
                }
        );
    }

    public void startProximityAdjustment() {
        mSensorHandler.registerProximityEvent();
        ImageDialog.showDialog(mActivity,
                mActivity.getString(R.string.sensor_adjustment_proximity),
                mActivity.getString(R.string.sensor_adjustment_proximity_text),
                mActivity.getString(R.string.sensor_adjustment_next),
                mActivity.getResources().getDrawable(R.drawable.hand_wave),
                new ImageDialog.ClickCallback() {
                    @Override
                    public void onClick() {
                        mStoreProximityValue = true;
                        Toast.makeText(mActivity,
                                R.string.sensor_adjustment_done, Toast.LENGTH_SHORT).show();
                        PreferencesHelper.setProximityValue(mActivity, mProximityValue);
                        mSensorHandler.unregisterProximityEvent();
                        mStoreProximityValue = false;
                    }
                }
        );
    }

    @Override
    public void onPocketModeChanged(boolean inPocket) {
        // stub
    }

    @Override
    public void onTableModeChanged(boolean onTable) {
        // stub
    }

    @Override
    public void onScreenStateChaged(boolean screenOn) {
        // stub
    }

    @Override
    public void onProximityValueReceived(float value) {
        if (!mStoreProximityValue) {
            mProximityValue = value;
        }
    }

    @Override
    public void onGyroscopeValuesReceived(float x, float y, float z) {
        // Table calibration
        if (mStoreTableValues) {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mTableValues = new float[]{Math.abs(x), Math.abs(y), Math.abs(z)};
            mStoreTableValues = false;
        }

        // Lifting calibration
        if (mStoreLiftingValue) {
            float threshold = 0;
            for (int i = 0; i < 3; i++) {
                if (mLiftValues != null) {
                    float delta = Math.abs(mLiftValues[i] - mTableValues[i]);
                    if (delta < threshold || threshold == 0) {
                        threshold = delta;
                    }
                } else {
                    threshold = PreferencesHelper.DEFAULT_MOTION_THRESHOLD;
                }
            }
            PreferencesHelper.setMotionThresholdValue(mActivity, threshold);
            mSensorHandler.unregisterGyroscopeEvent();
            mStoreLiftingValue = false;
        } else {
            if (mLiftValues == null ||
                    (Math.abs(x) > mLiftValues[0]
                            && Math.abs(y) > mLiftValues[1] && Math.abs(z) > mLiftValues[2])) {
                mLiftValues = new float[]{Math.abs(x), Math.abs(y), Math.abs(z)};
            }
        }
    }
}
