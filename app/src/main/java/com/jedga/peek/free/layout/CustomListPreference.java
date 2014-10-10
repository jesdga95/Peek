package com.jedga.peek.free.layout;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.jedga.peek.free.R;

public class CustomListPreference extends ListPreference {
    public CustomListPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.custom_list_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View iconView = view.findViewById(android.R.id.icon);
        if (iconView != null && iconView instanceof ImageView) {
            iconView.setClickable(true);
            iconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage(R.string.detection_info_message)
                            .setTitle(R.string.detection_info_title);
                    builder.setNeutralButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
    }
}