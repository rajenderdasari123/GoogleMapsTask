package com.example.directionsexample.adapter;

import android.view.View;

/**
 * Created by 2149 on 27-12-2017.
 */

public interface ClickListener {
    public void onClick(View view, int position);

    public void onLongClick(View view, int position);
}
