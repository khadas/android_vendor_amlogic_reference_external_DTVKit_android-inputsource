package com.droidlogic.fragment.dialog;

import android.app.AlertDialog;
import android.content.Context;
import java.util.List;
import android.util.Log;
import android.widget.AdapterView;
import android.text.TextUtils;

import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.fragment.LnbWrap;

public class DialogManager {

    private static final String TAG = "DialogManager";
    private String mCurrentType;
    private ParameterManager mParameterManager;
    private Context mContext;

    public DialogManager(Context context, ParameterManager manager) {
        this.mContext = context;
        this.mParameterManager = manager;
    }

    public void setCurrentType(String type) {
        mCurrentType = type;
    }

    public String getCurrentType() {
        return mCurrentType;
    }

    public CustomDialog buildItemDialogById(int id, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM, callBack, mParameterManager);
        if (id > CustomDialog.ID_DIALOG_TITLE_COLLECTOR.length - 1 || id > ParameterManager.ID_DIALOG_KEY_COLLECTOR.length - 1) {
            return null;
        }
        List<Integer> lnbParaValues = mParameterManager.getDvbsParaManager().getLnbParamsIntValue();
        if (TextUtils.equals(ParameterManager.ID_DIALOG_KEY_COLLECTOR[id], ParameterManager.KEY_UNICABLE_SWITCH)) {
            customDialog.initListView(mContext.getString(CustomDialog.ID_DIALOG_TITLE_COLLECTOR[id]), ParameterManager.KEY_UNICABLE, lnbParaValues.get(id));
        } else {
            customDialog.initListView(mContext.getString(CustomDialog.ID_DIALOG_TITLE_COLLECTOR[id]), ParameterManager.ID_DIALOG_KEY_COLLECTOR[id], lnbParaValues.get(id));
        }
        return customDialog;
    }

    public CustomDialog buildLnbCustomItemDialog(DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_ITEM, callBack, mParameterManager);
        customDialog.initLnbCustomItemDialog();
        return customDialog;
    }

    /*public CustomDialog buildUnicableCustomItemDialog(DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_ITEM, callBack, mParameterManager);
        customDialog.initUnicableCustomItemDialog();
        return customDialog;
    }*/

    public CustomDialog buildDiseqc1_2_ItemDialog(boolean isDiseqc1_3, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initDiseqc1_2_ItemDialog(isDiseqc1_3);
        return customDialog;
    }

    public CustomDialog buildAddTransponderDialogDialog(String parameter, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initAddTransponderDialog(parameter);
        return customDialog;
    }

    public CustomDialog buildRemoveTransponderDialogDialog(String parameter, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initRemoveTransponderDialog(parameter);
        return customDialog;
    }

    public CustomDialog buildAddSatelliteDialogDialog(String name, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initAddSatelliteDialog(name);
        return customDialog;
    }

    public CustomDialog buildRemoveSatelliteDialogDialog(String name, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initRemoveSatelliteDialog(name);
        return customDialog;
    }

    public CustomDialog buildAddAndEditLocatorDialog(String parameter, int position, String type, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM, callBack, mParameterManager);
        customDialog.initAddLocatorDialog(parameter, position, type);
        return customDialog;
    }
    /*public AlertDialog buildItemDialogByKey(String key, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM, callBack, mParameterManager);
        AlertDialog selectSingleItemDialog = customDialog.creatSelectSingleItemDialog(null, key, mParameterManager.getIntParameters(key));
        return selectSingleItemDialog;
    }*/

    /*public AlertDialog buildSelectSingleItemDialog(String title, DialogCallBack callBack) {
        CustomDialog customDialog = new CustomDialog(mContext, CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM, callBack, mParameterManager);
        AlertDialog selectSingleItemDialog = customDialog.creatSelectSingleItemDialog(title, null, mParameterManager.getSelectSingleItemValueIndex(title));
        return selectSingleItemDialog;
    }*/
}
