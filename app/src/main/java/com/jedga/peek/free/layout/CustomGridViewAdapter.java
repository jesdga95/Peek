/*
 * Copyright (C) 2013 Manish Srivastava.
 * This code has been modified. Portions Copyright (C) 2014 ParanoidAndroid Project.
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jedga.peek.free.R;
import com.jedga.peek.free.helpers.PackageHelper.AppContainer;
import com.jedga.peek.free.helpers.PreferencesHelper;

import java.util.List;

public class CustomGridViewAdapter extends ArrayAdapter<AppContainer> {

    private int mLayoutResourceId;

    public CustomGridViewAdapter(Context context, int layoutResourceId,
                                 List<AppContainer> data) {
        super(context, layoutResourceId, data);
        mLayoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        DrawerItemHolder holder;

        if (row == null) {
            LayoutInflater inflater
                    = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(mLayoutResourceId, parent, false);

            holder = new DrawerItemHolder();
            holder.appIcon = (ImageView) row.findViewById(R.id.item_image);
            holder.appName = (TextView) row.findViewById(R.id.item_text);
            row.setTag(holder);
        } else {
            holder = (DrawerItemHolder) row.getTag();
        }

        AppContainer app = getItem(position);
        final String packageName = app.appPackage;

        holder.appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isBlackListed
                        = PreferencesHelper.isPackageBlackListed(getContext(), packageName);
                PreferencesHelper.setPackageBlackListed(getContext(), packageName, !isBlackListed);
                view.animate().alpha(isBlackListed ? 1f : .2f);
            }
        });
        holder.appIcon.setAlpha(
                PreferencesHelper.isPackageBlackListed(getContext(), packageName) ? .2f : 1f);
        holder.appIcon.setImageDrawable(app.icon);
        holder.appIcon.setTag(packageName);
        holder.appName.setText(app.appName);
        return row;
    }

    static class DrawerItemHolder {
        ImageView appIcon;
        TextView appName;
    }
}