package com.droidlogic.dtvkit.inputsource.searchguide;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.FocusHighlightHelper;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.droidlogic.dtvkit.inputsource.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleListFragment extends com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment {
    private final String TAG = SimpleListFragment.class.getSimpleName();
    private ArrayObjectAdapter mListAdapter = null;
    private VerticalGridView mList = null;
    private List<String> mData = null;
    private HashMap<String, List<String>> mExtraData = new HashMap<>();
    private HashMap<String, String> mTransferData = new HashMap<>();
    private ProgressBar mProgressBar = null;
    private Button mNext = null;

    public static SimpleListFragment newInstance(String title) {
        SimpleListFragment fragment = new SimpleListFragment();
        fragment.setTitle(title);
        return fragment;
    }

    public static SimpleListFragment newInstance(String title, boolean spinType) {
        SimpleListFragment fragment = new SimpleListFragment();
        fragment.setTitle(title);
        Bundle bundle = new Bundle();
        bundle.putBoolean("isSpin", spinType);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.list_fragment, container, false);
        mList = view.findViewById(R.id.main_list);
//        mList.setNumColumns(1);
        mList.setItemSpacing(6);
        mList.setGravity(Gravity.CENTER_VERTICAL);
//        mList.setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
//            @Override
//            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subPosition) {
//                super.onChildViewHolderSelected(parent, child, position, subPosition);
//                if (child != null) {
//                    Toast.makeText(view.getContext(), child.itemView.toString() + " selected", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        Presenter presenter;
        boolean isSpinnerList = false;
        if (getArguments() != null) {
            isSpinnerList = (Boolean) getArguments().get("isSpin");
        }
        if (isSpinnerList) {
            presenter = new SpinPresenter();
        } else {
            presenter = new SimplePresenter();
        }
        mListAdapter = new ArrayObjectAdapter(presenter);
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(mListAdapter);
        mList.setAdapter(bridgeAdapter);
        mList.requestFocus();
        if (!isSpinnerList) {
            FocusHighlightHelper.setupBrowseItemFocusHighlight(bridgeAdapter, FocusHighlight.ZOOM_FACTOR_LARGE, false);
        } else {
            mNext = view.findViewById(R.id.next_button);
            mNext.setVisibility(View.VISIBLE);
            mNext.setFocusable(true);
            mNext.setOnClickListener(v -> {
                if (getListener() != null) {
                    Log.i(TAG, "next -> " + mTransferData.toString());
                    getListener().onNext(SimpleListFragment.this, mTransferData.toString(), 0);
                }
            });
            mNext.requestFocus();
        }

        TextView titleView = view.findViewById(R.id.main_title);
        titleView.setText(getTitle());
        mProgressBar = view.findViewById(R.id.loading_progress);
        mProgressBar.setVisibility(View.VISIBLE);
        Log.i(TAG, "onCreateView: " + getTag());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mProgressBar.setVisibility(View.VISIBLE);
        if (getListener() != null) {
            getListener().onFragmentReady(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mProgressBar.setVisibility(View.GONE);
    }

    public void updateDataWithExtra(HashMap<String, List<String>> data) {
        Log.i(TAG, "updateDataWithExtra size:" + data.size());
        mExtraData = data;
        mData = new ArrayList<>(data.keySet());
        if (mListAdapter != null) {
            mListAdapter.addAll(0, mData);
        }
        mProgressBar.setVisibility(View.GONE);
    }

    public void updateData(List<String> data, int position) {
        if (data == null) {
            Log.e(TAG, "invalid data");
            return;
        }
        mData = new ArrayList<>(data);
        if (data.size() == 0) {
            mData.add("No Data");
        }
        Log.i(TAG, "updateData size:" + data.size() + ", pos=" + position);
        if (position >= data.size()) {
            position = 0;
        }
        if (mListAdapter != null) {
            mListAdapter.addAll(0, mData);
            mList.scrollToPosition(position);
        } else {
            Log.e(TAG, "error:resource not ready");
        }
        mProgressBar.setVisibility(View.GONE);
    }

    private class SpinPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
            View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_spin, viewGroup, false);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object o) {
            TextView view = viewHolder.view.findViewById(R.id.title);
            view.setText(o.toString());
            Spinner content = viewHolder.view.findViewById(R.id.spinner_content);
            List<String> stringList = mExtraData.get(o.toString());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stringList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            content.setAdapter(adapter);
            content.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (stringList != null) {
                        String ss = stringList.get(position);
                        mTransferData.put(o.toString(), ss);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            content.setOnFocusChangeListener((v, hasFocus) -> {
                if (view.getText().toString().equals(mData.get(mData.size() - 1))) {
                    if (hasFocus) {
                        content.setOnKeyListener((v1, keyCode, event) -> {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                                mNext.requestFocus();
                                return true;
                            }
                            return false;
                        });
                    } else {
                        content.setOnKeyListener(null);
                    }
                }
            });
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {

        }
    }

    private class SimplePresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
            View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object o) {
            TextView view = viewHolder.view.findViewById(R.id.name);
            view.setText(o.toString());
            viewHolder.view.setOnClickListener(v -> {
                if (getListener() != null) {
                    getListener().onNext(SimpleListFragment.this, o.toString(), mData.indexOf(o.toString()));
                }
            });
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {

        }
    }
}
