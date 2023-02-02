package org.dtvkit.inputsource.searchguide;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.dtvkit.inputsource.searchguide.DataPresenter;
import com.droidlogic.dtvkit.inputsource.searchguide.TKGSMenuSettingFragment;
import com.droidlogic.fragment.DvbsParameterManager;
import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.fragment.dialog.CustomDialog;
import com.droidlogic.fragment.dialog.DialogCallBack;
import com.droidlogic.fragment.dialog.DialogManager;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class TKGSLocatorListFragment extends com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment {
    private final static String TAG = TKGSLocatorListFragment.class.getSimpleName();
    private VerticalGridView mVerticalGridView;
    private ArrayObjectAdapter mAdapter;
    private List<String> mData;
    private int mSelectPosition = 0;

    public static TKGSLocatorListFragment newInstance(String title) {
        return newInstance(title, null);
    }

    public static TKGSLocatorListFragment newInstance(String title, com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener listener) {
        TKGSLocatorListFragment fragment = new TKGSLocatorListFragment();
        fragment.setTitle(title);
        fragment.setListener(listener);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_tkgs_locator_list, container, false);
        TextView title = view.findViewById(R.id.locator_title);
        TextView tipsView = view.findViewById(R.id.tip_message_tv);
        if (DataPresenter.FRAGMENT_KTGS_LOCATOR_LIST.equals(getTitle())) {
            tipsView.setText(R.string.string_tkgs_locator_edit);
        } else if (DataPresenter.FRAGMENT_KTGS_HIDDEN_LOCATORS.equals(getTitle())) {
            tipsView.setVisibility(View.GONE);
        }
        title.setText(getTitle());
        mVerticalGridView = (VerticalGridView) view.findViewById(R.id.locator_list);
        Presenter presenter = new FunctionPresenter();
        mAdapter = new ArrayObjectAdapter(presenter);
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(mAdapter);
        mVerticalGridView.setAdapter(bridgeAdapter);
        mVerticalGridView.requestFocus();
        setCanBackToPrevious(true);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mData != null && mData.size() > 0) {
            updateData(mData, 0);
            return;
        }
        if (getListener() != null) {
            getListener().onFragmentReady(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mData != null) {
            mData.clear();
        }
    }

    public void updateData(List<String> dataList, int position) {
        Log.i(TAG,"updateData " + dataList.size() + " " + position);
        mData = new ArrayList<>();
        mData.addAll(dataList);
        if (DataPresenter.FRAGMENT_KTGS_LOCATOR_LIST.equals(getTitle())) {
            mData.add("Add a new locator");
        } else if (DataPresenter.FRAGMENT_KTGS_HIDDEN_LOCATORS.equals(getTitle())) {
            mData.add("Clear All Hidden Locators");
        }
        if (mSelectPosition >= mData.size()) {
            mSelectPosition = 0;
        }
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.addAll(0, mData);
            mVerticalGridView.scrollToPosition(mSelectPosition);
        } else {
            Log.w(TAG, "warning:view not ready");
        }
    }


    private class FunctionPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
            View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.tkgs_locator_item, viewGroup, false);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object o) {
            TextView indexTv = viewHolder.view.findViewById(R.id.index);
            TextView locatorContentTv = viewHolder.view.findViewById(R.id.content);
            indexTv.setText(String.valueOf(mData.indexOf(o.toString()) + 1));
            if (mData.indexOf(o.toString()) == mData.size() -1) {
                indexTv.setVisibility(View.INVISIBLE);
            } else {
                indexTv.setVisibility(View.VISIBLE);
            }
            locatorContentTv.setText(o.toString());
            viewHolder.view.setTag(o);
            viewHolder.view.setOnClickListener(onClickListener);
            if (DataPresenter.FRAGMENT_KTGS_LOCATOR_LIST.equals(getTitle()) && !"Add a new locator".equals(o.toString())) {
                viewHolder.view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showEditAndDeleteSelectDialog(getContext(), o.toString(), mData.indexOf(o.toString()));
                        return false;
                    }
                });
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {

        }

    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ("Add a new locator".equals(view.getTag())) {
                showAddAndEditLocatorDialog("", -1, "Add");
                mSelectPosition = mData.size() - 1;
            } else if ("Clear All Hidden Locators".equals(view.getTag())) {
                getParameterManager().setTKGSHiddenTpLocation();
                getListener().onFragmentReady(TKGSLocatorListFragment.this);
            }
        }
    };

    private void showAddAndEditLocatorDialog(String parameter, int position, String type) {
        Log.i(TAG,"showAddAndEditLocatorDialog parameter : " + parameter);
        CustomDialog mCurrentCustomDialog = getDialogManager().buildAddAndEditLocatorDialog(parameter, position, type, mSingleSelectDialogCallBack);
        mCurrentCustomDialog.showDialog();
    }

    private final DialogCallBack mSingleSelectDialogCallBack = new DialogCallBack() {
        @Override
        public void onStatusChange(View view, String dialogType, Bundle data) {
            if (ParameterManager.KEY_ADD_LOCATOR.equals(dialogType)) {
                mSelectPosition = mSelectPosition + 1;
            }
            getListener().onFragmentReady(TKGSLocatorListFragment.this);
        }
    };

    private void showEditAndDeleteSelectDialog(final Context context, String locator, int position) {
        if (context == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.confirm_search, null);
        final TextView title = dialogView.findViewById(R.id.dialog_title);
        final Button confirm = dialogView.findViewById(R.id.confirm);
        final Button cancel = dialogView.findViewById(R.id.cancel);
        title.setText(R.string.string_select_edit_method);
        confirm.setText("Edit");
        cancel.setText("Delete");
        confirm.requestFocus();
        cancel.setOnClickListener(v -> {
            mSelectPosition = position - 1;
            JSONArray jsonArray = getParameterManager().getTKGSVisibleLocatorsList();
            jsonArray.remove(position);
            getParameterManager().setTKGSVisibleLocators(jsonArray);
            alert.dismiss();
            getListener().onFragmentReady(TKGSLocatorListFragment.this);
        });
        confirm.setOnClickListener(v -> {
            mSelectPosition = position;
            showAddAndEditLocatorDialog(locator, position, "Edit");
            alert.dismiss();
        });
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }
}
