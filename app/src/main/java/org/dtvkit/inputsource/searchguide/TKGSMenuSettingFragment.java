package com.droidlogic.dtvkit.inputsource.searchguide;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.settings.PropSettingManager;

import java.util.Arrays;

public class TKGSMenuSettingFragment extends com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment {
    private final static String TAG = TKGSMenuSettingFragment.class.getSimpleName();

    public static TKGSMenuSettingFragment newInstance(String title) {
        return newInstance(title, null);
    }

    public static TKGSMenuSettingFragment newInstance(String title, com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener listener) {
        TKGSMenuSettingFragment fragment = new TKGSMenuSettingFragment();
        fragment.setTitle(title);
        fragment.setListener(listener);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_tkgs_menu, container, false);
        TextView titleView = (TextView) view.findViewById(R.id.tkgs_title);
        TextView locatorListView = (TextView) view.findViewById(R.id.locator_list_tv);
        TextView updateView = (TextView) view.findViewById(R.id.update_tv);
        TextView hiddenLocatorView = (TextView) view.findViewById(R.id.hidden_locator_list_tv);
        LinearLayout table_version_ll = (LinearLayout) view.findViewById(R.id.table_version_ll);
        LinearLayout prefer_list_ll = (LinearLayout) view.findViewById(R.id.prefer_ll);
        TextView tableVersionView = (TextView) view.findViewById(R.id.table_version_tv);
        Spinner operateModeSpinner = (Spinner)view.findViewById(R.id.operate_mode_spinner);
        Spinner preferListSpinner = (Spinner)view.findViewById(R.id.prefer_spinner);
        titleView.setText(getTitle());
        String[] preferSpinnerValues = getParameterManager().getTKGSAllPreferList().toArray(new String[0]);
        String[] operatorModeValues = {"auto", "custom", "off"};
        ArrayAdapter<String> adapter;

        if (PropSettingManager.getBoolean(PropSettingManager.TKGS_DEBUG_ENABLE, true)) {
            hiddenLocatorView.setVisibility(View.VISIBLE);
            table_version_ll.setVisibility(View.VISIBLE);
            tableVersionView.setVisibility(View.VISIBLE);
            int version = getParameterManager().getTKGSVersion();
            tableVersionView.setText((version == -1 || version == 255) ? "None" : String.valueOf(version));
        }

        locatorListView.setOnClickListener(v -> {
            if (getListener() != null) {
                getListener().onNext(TKGSMenuSettingFragment.this, ((TextView) v).getText().toString());
            }
        });

//        if (getParameterManager().getChannelIdForSource() == -1) {
//            updateView.setEnabled(false);
//        }

        updateView.setOnClickListener(v -> {
            if (getListener() != null) {
                getListener().onNext(TKGSMenuSettingFragment.this, ((TextView) v).getText().toString());
            }
        });

        hiddenLocatorView.setOnClickListener(v -> {
            if (getListener() != null) {
                getListener().onNext(TKGSMenuSettingFragment.this, ((TextView) v).getText().toString());
            }
        });

        table_version_ll.setOnClickListener(v -> {
            getParameterManager().resetTKGSVersion("ver");
            int version = getParameterManager().getTKGSVersion();
            tableVersionView.setText((version == -1 || version == 255) ? "None" : String.valueOf(version));
        });

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, operatorModeValues);
        operateModeSpinner.setAdapter(adapter);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        operateModeSpinner.setSelection(Arrays.asList(operatorModeValues).indexOf(getParameterManager().getTKGSOperatingMode()));
        operateModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                getParameterManager().setTKGSOperatingMode(operatorModeValues[position]);
                getParameterManager().saveStringParameters(ParameterManager.TKGS_OPERATOR_MODE, operatorModeValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, preferSpinnerValues);
        if (preferSpinnerValues.length == 0) {
            prefer_list_ll.setVisibility(View.GONE);
        }
        preferListSpinner.setAdapter(adapter);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        preferListSpinner.setSelection( Arrays.asList(preferSpinnerValues).indexOf(getParameterManager().getTKGSSelectPreferList()));
        preferListSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                getParameterManager().setTKGSServiceList(preferSpinnerValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Button scan = (Button)view.findViewById(R.id.bt_scan);
        scan.setOnClickListener(v -> {
            if (getListener() != null) {
                getListener().onNext(TKGSMenuSettingFragment.this, ((TextView) v).getText().toString());
            }
        });
        setCanBackToPrevious(true);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

}
