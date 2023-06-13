package org.dtvkit.inputsource.fvp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.dtvkit.inputsource.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClmDialogFragment extends DialogFragment {
    private static final String TAG = "ClmDialogFragment";
    private static final String DIALOG_TYPE = "DIALOG_TYPE";
    public static final int DIALOG_TYPE_USER_SELECTION = 0;
    public static final int DIALOG_TYPE_POST_CODE_INPUT = 1;
    public static final int DIALOG_TYPE_TRD_SELECTION = 2;

    private int mDialogType = DIALOG_TYPE_USER_SELECTION;
    private Context mContext = null;
    private View mDialogView = null;
    private TextView mTitle = null;
    private TextView mContent = null;
    private EditText mPostCodeInput = null;
    private Button mOKButton = null;
    private Button mCancelButton = null;
    private Spinner mRegionSpinner = null;
    private ClmManager mClmManager = null;
    private String mPostCode = "";
    private int mDefaultRegionOrder = 0;

//    public static ClmDialogFragment create(int type) {
//        ClmDialogFragment fragment = new ClmDialogFragment();
//        Bundle args = new Bundle();
//        args.putInt(DIALOG_TYPE, type);
//        fragment.setArguments(args);
//        return fragment;
//    }

    public ClmDialogFragment(){}

    public void setClmManager(ClmManager clmManager) {
        getActivity();
        mClmManager = clmManager;
        mContext = mClmManager.getContext();
        mClmManager.addCallback(new ClmManager.ClmDialogUpdateCallback() {
            @Override
            public void showUserSelection(String dialogQuestion, Map orderMap) {
                if (null == mDialogView) {
                    Log.d(TAG, "mDialogView not init");
                    return;
                }
                mContent.setText(dialogQuestion);
                //can't click twice
                mOKButton.setClickable(true);
                mCancelButton.setClickable(true);
                if (2 == orderMap.size()) {
                    Pair<Integer, String> orderContent1 = (Pair<Integer, String>) orderMap.get(0);
                    mOKButton.setText(orderContent1.second);
                    mOKButton.setOnClickListener((View v)->{
                        if (null != mClmManager) {
                            mClmManager.setUserSelection(orderContent1.first);
                        }
                        mOKButton.setClickable(false);
                    });
                    Pair<Integer, String> orderContent2 = (Pair<Integer, String>) orderMap.get(1);
                    mCancelButton.setText(orderContent2.second);
                    mCancelButton.setOnClickListener((View v)->{
                        if (null != mClmManager) {
                            mClmManager.setUserSelection(orderContent2.first);
                        }
                        mCancelButton.setClickable(false);
                    });
                }
                changeDialogTypeView(DIALOG_TYPE_USER_SELECTION);
            }

            @Override
            public void showPostCode() {
                //init input post code
                mPostCode = "";
                //can't click twice
                mCancelButton.setClickable(true);
                mCancelButton.setOnClickListener((View v)->{
                    if (null != mClmManager) {
                        mClmManager.setRegionId(mPostCode);
/*
                        if (null != mPostCode) {
                            //int postCode = Integer.valueOf(mPostCode);
                            mClmManager.setRegionId(mPostCode);
                        } else {
                            mClmManager.setRegionId(null);
                        }
*/
                    }
                    mCancelButton.setClickable(false);
                });
                changeDialogTypeView(DIALOG_TYPE_POST_CODE_INPUT);
            }

            @Override
            public void showRegionSelection(String dialogQuestion, Map<Integer, Pair<Integer, String>> orderMap) {
                Log.d(TAG, "showRegionSelection");
                mTitle.setText(dialogQuestion);
                mContent.setText(dialogQuestion);
                Map<String, Integer> regionMap = new HashMap<>();
                ArrayAdapter<String> regionAdapter = new ArrayAdapter(mContext, android.R.layout.simple_spinner_item);
                regionAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
                orderMap.forEach((Integer key, Pair<Integer, String> value)-> {
                    Log.d(TAG, "order : " + value.first + " region : " + value.second);
                    regionMap.put(value.second, value.first);
                    regionAdapter.add(value.second);
                });
                mRegionSpinner.setAdapter(regionAdapter);
 /*
                mRegionSpinner.setOnItemClickListener((adapterView, view, position, rowId)-> {
                    Log.d(TAG, "click region item position : " + position);
                    String selectRegion = regionAdapter.getItem(position);
                    int order = regionMap.get(selectRegion);
                    if (null != mClmManager) {
                        mClmManager.setUserSelection(order);
                    }
                });
 */
                mRegionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(TAG, "OnItemSelectedListener position : " + position);
                        String selectRegion = regionAdapter.getItem(position);
                        mDefaultRegionOrder = regionMap.get(selectRegion);
                        //if (null != mClmManager) {
                        //    mClmManager.setUserSelection(order);
                        //}
                        Log.d(TAG, "OnItemSelectedListener position : " + position + " mDefaultRegionOrder :" + mDefaultRegionOrder);
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.d(TAG, "onNothingSelected position");
                    }
                });
                //can't click twice
                mCancelButton.setClickable(true);
                mCancelButton.setOnClickListener((View v)->{
                    if (null != mClmManager) {
                        mClmManager.setUserSelection(mDefaultRegionOrder);
                    }
                    mCancelButton.setClickable(false);
                });

                changeDialogTypeView(DIALOG_TYPE_TRD_SELECTION);
            }

            @Override
            public void clmHandleFinish() {
                dismiss();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mDialogType = getArguments().getInt(DIALOG_TYPE, DIALOG_TYPE_USER_SELECTION);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dlg = getDialog();
        if ((null != dlg) || (null != dlg.getWindow())) {
            Log.d(TAG, "set keylistener");
            dlg.getWindow().setLayout(getResources().getDimensionPixelSize(R.dimen.clm_dialog_width),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dlg.setOnKeyListener((DialogInterface dialog, int keyCode, KeyEvent event)->{
                Log.d(TAG, "setOnKeyListener event: " + event);
                if (KeyEvent.ACTION_UP == event.getAction() && KeyEvent.KEYCODE_BACK  == keyCode) {
                    if (null != mClmManager) {
                        mClmManager.exitIpScan(false);
                    }
                    dismissAllowingStateLoss();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.clm_dialog_normal, container, false);
        mTitle = v.findViewById(R.id.clm_title);
        mContent = v.findViewById(R.id.clm_content);
        mOKButton = v.findViewById(R.id.btn_ok);
        mCancelButton = v.findViewById(R.id.btn_cancel);
        mRegionSpinner = v.findViewById(R.id.spinner_fvp_region);
        mPostCodeInput = v.findViewById(R.id.post_code_input);

        mPostCodeInput.addTextChangedListener (new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged = " + s.toString() + "start = " + start + "before = " + before + "count = " + count);
                mPostCode = s.toString();
                //checkInputPostCode(s, start);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (null != s) {
                    Log.d(TAG, "afterTextChanged = " + mPostCode);
                }
            }
        });
        mDialogView = v;
        mDialogView.setVisibility(View.GONE);
        return v;
    }

    public void changeDialogTypeView(int dialogType) {
        Log.d(TAG, "changeDialogTypeView Old Type : " + mDialogType + " new type : " + dialogType);
        mDialogType = dialogType;
        if (DIALOG_TYPE_USER_SELECTION == mDialogType) {
            mPostCodeInput.setVisibility(View.GONE);
            mRegionSpinner.setVisibility(View.GONE);
            mContent.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mOKButton.setVisibility(View.VISIBLE);
            mOKButton.requestFocus();
            mTitle.setText("Please User Selection");
        } else if (DIALOG_TYPE_POST_CODE_INPUT == mDialogType) {
            mContent.setVisibility(View.GONE);
            mRegionSpinner.setVisibility(View.GONE);
            mOKButton.setVisibility(View.INVISIBLE);
            mPostCodeInput.setVisibility(View.VISIBLE);
            mPostCodeInput.requestFocus();
            mCancelButton.setVisibility(View.VISIBLE);
            mCancelButton.setText("OK");
            mTitle.setText("Please Code Input");
        } else if (DIALOG_TYPE_TRD_SELECTION == mDialogType) {
            mPostCodeInput.setVisibility(View.GONE);
            mOKButton.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
            mContent.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            mRegionSpinner.setVisibility(View.VISIBLE);
            mRegionSpinner.requestFocus();
            mCancelButton.setText("OK");
            //mTitle.setText("Select your preferred region");
        }
        if (View.VISIBLE != mDialogView.getVisibility()) {
            mDialogView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss");
    }

    private void checkInputPostCode(CharSequence s, int inputStart) {
        if (TextUtils.isDigitsOnly(s.toString())) {
            mPostCode = s.toString();
        } else {
            Toast.makeText(getActivity(), "Please Input Digits Only", Toast.LENGTH_SHORT).show();
            ((Editable)s).delete(inputStart,inputStart+1);
        }
    }
}
