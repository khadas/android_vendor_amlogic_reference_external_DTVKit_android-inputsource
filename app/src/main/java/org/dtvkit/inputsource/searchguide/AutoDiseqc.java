package com.droidlogic.dtvkit.inputsource.searchguide;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener;
import com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment;
import org.json.JSONObject;

public class AutoDiseqc extends SearchStageFragment {
    private final String TAG = AutoDiseqc.class.getSimpleName();
    private ProgressBar mProgressBar = null;
    private Button mContinueBt = null;
    private Button mSkipBt = null;

    public static AutoDiseqc newInstance(String title) {
        return newInstance(title, null);
    }

    public static AutoDiseqc newInstance(String title, OnNextListener listener) {
        AutoDiseqc fragment = new AutoDiseqc();
        fragment.setTitle(title);
        fragment.setListener(listener);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_autodiseqc, container, false);
        TextView titleView = (TextView) view.findViewById(R.id.main_title);
        titleView.setText(getTitle());
        mContinueBt = (Button) view.findViewById(R.id.bt_continue);
        mContinueBt.setOnClickListener(v -> {
            if (getListener() != null) {
                getListener().onNext(AutoDiseqc.this, ((TextView) v).getText().toString());
            }
        });
        mSkipBt = (Button) view.findViewById(R.id.bt_skip);
        mSkipBt.setOnClickListener((v) -> {
            if (getListener() != null) {
                getListener().onNext(AutoDiseqc.this, ((TextView) v).getText().toString());
            }
        });
        mContinueBt.requestFocus();
        mProgressBar = view.findViewById(R.id.loading_progress);
        return view;
    }

    @Override
    public void handleMessage(String signal, JSONObject object) {
        // when get Message
        Log.i(TAG, "message:" + object);
        mProgressBar.setVisibility(View.GONE);
        if (getListener() != null) {
            // when satisfied, go to next fragment
            getListener().onNext(AutoDiseqc.this, "info");
        }
    }
}
