package com.droidlogic.dtvkit.inputsource.searchguide;

import android.app.Fragment;
import android.support.annotation.Nullable;

import org.json.JSONArray;

public interface OnNextListener {
    void onNext(Fragment fragment, String text);

    void onNext(Fragment fragment, String text, int pos);

    void onNext(Fragment fragment, String text, @Nullable JSONArray array);

    void onFragmentReady(Fragment fragment);
}
