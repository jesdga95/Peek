/*
 * Copyright (C) 2013 ParanoidAndroid Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.jedga.peek.free.R;

public class ImageDialog {

    public static void showDialog(final Context context, String title,
                                  String msg, String buttonText, Drawable hint, final ClickCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View dialogLayout = layoutInflater.inflate(R.layout.image_dialog, null);
        dialogLayout.setPadding(10, 10, 10, 20);
        final ImageView visualHint = (ImageView)
                dialogLayout.findViewById(R.id.dialog_image);
        visualHint.setImageDrawable(hint);
        builder.setView(dialogLayout);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(buttonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onClick();
                    }
                }
        );
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static interface ClickCallback {
        public abstract void onClick();
    }

}