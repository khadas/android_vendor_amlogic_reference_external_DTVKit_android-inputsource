package com.droidlogic.dtvkit.inputsource.searchguide;

import android.app.Fragment;

public interface OnNextListener {
    void onNext(Fragment fragment, String text);

    void onNext(Fragment fragment, String text, int pos);

    void onFragmentReady(Fragment fragment);
}
