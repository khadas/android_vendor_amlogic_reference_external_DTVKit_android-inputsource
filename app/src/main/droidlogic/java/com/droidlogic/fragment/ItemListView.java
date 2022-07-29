package com.droidlogic.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;
import android.text.TextUtils;

import com.droidlogic.fragment.dialog.DialogCallBack;
import com.droidlogic.fragment.ItemAdapter.ItemDetail;

import java.util.LinkedList;

import com.droidlogic.dtvkit.inputsource.R;
import org.droidlogic.dtvkit.DtvkitGlueClient;

public class ItemListView extends ListView implements OnItemSelectedListener {
    private static final String TAG = "ItemListView";

    public static final String ITEM_SATELLITE              = ParameterManager.ITEM_SATELLITE;//"item_satellite";
    public static final String ITEM_TRANSPONDER            = ParameterManager.ITEM_TRANSPONDER;//"item_transponder";
    public static final String ITEM_LNB                    = ParameterManager.ITEM_LNB;//"item_lnb";
    public static final String ITEM_SATELLITE_OPTION       = ParameterManager.ITEM_SATELLITE_OPTION;//"item_satellite_option";
    public static final String ITEM_TRANSPONDER_OPTION     = ParameterManager.ITEM_TRANSPONDER_OPTION;//"item_transponder_option";
    public static final String ITEM_OPTION                 = ParameterManager.ITEM_OPTION;

    private Context mContext;
    private int selectedPosition = 0;
    private String mListType = ITEM_LNB;
    private ViewGroup rootView = null;
    private ListItemSelectedListener mListItemSelectedListener;
    private ListItemFocusedListener mListItemFocusedListener;
    private ListSwitchedListener mListSwitchedListener;
    private ListTypeSwitchedListener mListTypeSwitchedListener;
    private DialogCallBack mDataCallBack;

    private ParameterManager mParameterManager;

    public static final String LIST_LEFT = "left";
    public static final String LIST_RIGHT = "right";
    public static final String LIST_MIDDLE = "middle";
    private String mCurrentListSide = LIST_LEFT;

    public ItemListView(Context context) {
        super(context);
        mContext = context;
        setRootView();
        mParameterManager = new ParameterManager(mContext, null);
    }
    public ItemListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setRootView();
        setOnItemSelectedListener(this);
        mParameterManager = new ParameterManager(mContext, null);
    }

    public void setDtvkitGlueClient(DtvkitGlueClient client) {
        mParameterManager.setDtvkitGlueClient(client);
    }

    public void setCurrentListSide(String currentListSide) {
        mCurrentListSide = currentListSide;
    }

    public boolean dispatchKeyEvent (KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent mCurrentListSide = " + mCurrentListSide + ", event = " + event);
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                Bundle bundle1 = new Bundle();
                bundle1.putString("action", "scan");
                bundle1.putInt("keycode", event.getKeyCode());
                mDataCallBack.onStatusChange(null, ParameterManager.KEY_FUNCTION, bundle1);
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_PROG_RED:
                case KeyEvent.KEYCODE_PROG_GREEN:
                case KeyEvent.KEYCODE_PROG_BLUE:
                case KeyEvent.KEYCODE_PROG_YELLOW:
                    if (mDataCallBack != null) {
                        Bundle bundle1 = new Bundle();
                        String function = "";
                        if (event.getKeyCode() == KeyEvent.KEYCODE_PROG_RED) {
                            function = "add";
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_PROG_GREEN) {
                            function = "edit";
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_PROG_BLUE) {
                            function = "scan";
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_PROG_YELLOW) {
                            function = "delete";
                        }
                        bundle1.putString("action", function);
                        bundle1.putInt("keycode", event.getKeyCode());
                        bundle1.putString("listType", isLeftList() ?  ItemListView.ITEM_LNB : mParameterManager.getDvbsParaManager().getCurrentListType());
                        String parameter = "";
                        View selectedView1 = getSelectedView();
                        TextView textview = null;
                        if (selectedView1 != null) {
                            textview = (TextView) selectedView1.findViewById(R.id.textview_second);
                        }
                        if (isSatelliteList(mListType)) {
                            parameter = (textview != null && textview.getText() != null) ? textview.getText().toString() : "";//mParameterManager.getCurrentSatellite();
                        } else if (isTransponderList(mListType)) {
                            parameter = (textview != null && textview.getText() != null) ? textview.getText().toString() : "";//mParameterManager.getCurrentTransponder();
                        } else if (isRightList()) {
                            LinkedList<ItemDetail> satelliteList = mParameterManager.getDvbsParaManager().getSatelliteNameList();
                            LinkedList<ItemDetail> transponderList = mParameterManager.getDvbsParaManager().getTransponderList();
                            String currentList = mParameterManager.getDvbsParaManager().getCurrentListType();
                            if ((TextUtils.equals(currentList, ItemListView.ITEM_SATELLITE) && satelliteList != null && satelliteList.size() > 0) ||
                                    (TextUtils.equals(currentList, ItemListView.ITEM_TRANSPONDER) && transponderList != null && transponderList.size() > 0)) {
                                if (!(event.getKeyCode() == KeyEvent.KEYCODE_PROG_BLUE)) {
                                    return true;
                                }
                            } else {
                                bundle1.putString("listType", mParameterManager.getDvbsParaManager().getCurrentListType());//need to add new device
                            }
                        }
                        bundle1.putString("parameter", parameter);
                        mDataCallBack.onStatusChange(getSelectedView(), ParameterManager.KEY_FUNCTION, bundle1);
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_STOP:
                    if (isRightList() || isLeftList()) {
                        return true;
                    }
                    String savedListType = mParameterManager.getDvbsParaManager().getCurrentListType();
                    String leftListType = TextUtils.isEmpty(savedListType) ? mListType : savedListType;
                    String result = switchListType(leftListType);
                    if (LIST_LEFT.equals(mCurrentListSide) && result != null) {
                        mListType = result;
                    }
                    if (mListTypeSwitchedListener != null && result != null) {
                        mListTypeSwitchedListener.onListTypeSwitched(result);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (selectedPosition == 0)
                        return true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (selectedPosition == getAdapter().getCount() - 1)
                        return true;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    Log.d(TAG, "KEYCODE_DPAD_LEFT mListSwitchedListener = " + mListSwitchedListener + ", mListType = " + mListType + ", isLeftList = " + isLeftList());
                    if (isLeftList()) {
                        /*String savedListType = mParameterManager.getDvbsParaManager().getCurrentListType();
                        String leftListType = TextUtils.isEmpty(savedListType) ? mListType : savedListType;
                        String result = switchListType(leftListType);
                        Log.d(TAG, "mListSwitchedListener: switchList result = " + result + ", currentListType = " + savedListType);
                        if (LIST_LEFT.equals(mCurrentListSide) && result != null) {
                            mListType = result;
                        }
                        if (mListTypeSwitchedListener != null && result != null) {
                            mListTypeSwitchedListener.onListTypeSwitched(result);
                        }*/
                        mParameterManager.getDvbsParaManager().setCurrentListDirection("left");
                        return true;
                    } else if (isRightList()) {
                        /*LinkedList<ItemDetail> satelliteList = mParameterManager.getSatelliteList();
                        LinkedList<ItemDetail> transponderList = mParameterManager.getTransponderList();
                        String currentList = mParameterManager.getCurrentListType();
                        if (!((satelliteList != null && satelliteList.size() > 0) ||
                                (transponderList != null && transponderList.size() > 0))) {
                            Log.d(TAG, "no satellite or transponder!");
                            return true;
                        }*/
                        if (mListSwitchedListener != null) {
                            mListSwitchedListener.onListSwitched(LIST_MIDDLE);
                        }
                        return true;
                    } else if (isMiddleList()) {
                        if (mListSwitchedListener != null) {
                            mListSwitchedListener.onListSwitched(LIST_LEFT);
                        }
                        mParameterManager.getDvbsParaManager().setCurrentListDirection("left");
                    }
                    return super.dispatchKeyEvent(event);
                    //break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    Log.d(TAG, "KEYCODE_DPAD_RIGHT mListSwitchedListener = " + mListSwitchedListener + ", mListType = " + mListType);
                    /*if (isLeftList() || isRightList()) {
                        if (mListSwitchedListener != null) {
                            mListSwitchedListener.onListSwitched(LIST_RIGHT);
                        }
                        return true;
                    }*/
                    if (isLeftList()) {
                        if (mListSwitchedListener != null) {
                            mListSwitchedListener.onListSwitched(LIST_MIDDLE);
                        }
                        return true;
                    } else if (isMiddleList() || isRightList()) {
                        if (mListSwitchedListener != null) {
                            mListSwitchedListener.onListSwitched(LIST_RIGHT);
                        }
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                    //break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (isLeftList()) {
                        if (mListItemSelectedListener != null) {
                            mListItemSelectedListener.onListItemSelected(getSelectedItemPosition(), mListType, true);
                        }
                        return true;
                    } else if (isRightList()) {
                        if (mListItemSelectedListener != null) {
                            mListItemSelectedListener.onListItemSelected(getSelectedItemPosition(), mListType, true);
                        }
                        return true;
                    } else if (isMiddleList()) {
                        boolean selected = true;
                        ItemDetail item = (ItemDetail)getAdapter().getItem(getSelectedItemPosition());
                        if (ITEM_SATELLITE.equals(mListType) || ITEM_TRANSPONDER.equals(mListType)) {
                            if (item.getEditStatus() != ItemDetail.SELECT_EDIT) {
                                item.setEditStatus(ItemDetail.SELECT_EDIT);
                                selected = true;
                            } else {
                                item.setEditStatus(ItemDetail.NOT_SELECT_EDIT);
                                selected = false;
                            }
                        }
                        if (mListItemSelectedListener != null) {
                            mListItemSelectedListener.onListItemSelected(getSelectedItemPosition(), mListType, selected);
                        }
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                    //break;
                default:
                    break;
            }

            View selectedView = getSelectedView();
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if ( selectedView != null) {
                        clearChoosed(selectedView);
                    }
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if ( selectedView != null) {
                        //setItemTextColor(selectedView, false);
                        //clearChoosed(selectedView);
                    }
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected mCurrentListSide = " + mCurrentListSide + ", mListType = " + mListType +", position = " + position + ", id = " + id + ", view = " + view);
        selectedPosition = position;
        if (view != null) {
            /*if (hasFocus()) {
                setItemTextColor(view, true);
            } else {
                setItemTextColor(view, false);
            }*/
            cleanChoosed();
            setChoosed(view);
        }
        if (view != null && mListItemFocusedListener != null) {
            mListItemFocusedListener.onListItemFocused(/*(View)parent*/view, position, mListType);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /*@Override
    protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        Log.d(TAG, "onFocusChanged mListType = " + mListType + ", gainFocus = " + gainFocus +", direction = " + direction);
        View item = getSelectedView();
        if (item != null) {
            if (gainFocus) {
                cleanChoosed();
                //setItemTextColor(item, true);
                setChoosed(item);
            } else {
                //setItemTextColor(item, false);
                clearChoosed(item);
            }
        }
    }*/

    private void setItemTextColor (View view, boolean focused) {
        if (focused) {
            int color_text_focused = mContext.getResources().getColor(R.color.common_focus);
            for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
                View child = ((ViewGroup)view).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView)child).setTextColor(color_text_focused);
                }
            }
        } else {
            int color_text_item = mContext.getResources().getColor(R.color.common_item_background);
            for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
                View child = ((ViewGroup)view).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView)child).setTextColor(color_text_item);
                }
            }
        }
    }

    private void setRootView() {
        rootView = ((ViewGroup)((Activity)mContext).findViewById(android.R.id.content));
    }

    private boolean hasNextFocusView(int dec) {
        if (FocusFinder.getInstance().findNextFocus(rootView, this, dec) == null) {
            return false;
        } else {
            return true;
        }
    }

    public void cleanChoosed() {
        /*RuntimeException e = new RuntimeException("cleanChoosed is here " + mListType);
        e.fillInStackTrace();
        Log.d(TAG, "@@@@@@@@ ", e);*/
        int color_text_item = mContext.getResources().getColor(R.color.common_item_background);
        for (int i = 0; i < getChildCount(); i ++) {
            View view = getChildAt(i);
            //Log.d(TAG, "cleanChoosed all view = " + view + ", i = " + i);
            view.setBackgroundColor(color_text_item);
        }
    }

    public void clearChoosed(View view) {
        /*RuntimeException e = new RuntimeException("clearChoosed is here " + mListType);
        e.fillInStackTrace();
        Log.d(TAG, "@@@@@@@@ ", e);*/
        int color_text_item = mContext.getResources().getColor(R.color.common_item_background);
        view.setBackgroundColor(color_text_item);
    }

    public void setChoosed(View view) {
        /*RuntimeException e = new RuntimeException("setChoosed is here " + mListType);
        e.fillInStackTrace();
        Log.d(TAG, "@@@@@@@@ ", e);*/
        int color_text_focused = mContext.getResources().getColor(R.color.common_focus);
        view.setBackgroundColor(color_text_focused);
    }

    public void setListType(String type) {
        mListType = type;
    }

    public void setListItemSelectedListener(ListItemSelectedListener l) {
        mListItemSelectedListener = l;
    }

    public void setListItemFocusedListener(ListItemFocusedListener l) {
        mListItemFocusedListener = l;
    }

    public void setListSwitchedListener(ListSwitchedListener l) {
        mListSwitchedListener = l;
    }

    public void setListTypeSwitchedListener(ListTypeSwitchedListener l) {
        mListTypeSwitchedListener = l;
    }

    public void setDataCallBack(DialogCallBack callback) {
        mDataCallBack = callback;
    }

    public interface ListItemSelectedListener {
        void onListItemSelected(int position, String type, boolean selected);
    }

    public interface ListItemFocusedListener {
        void onListItemFocused(View parent, int position, String type);
    }

    public interface ListSwitchedListener {
        void onListSwitched(String direction);
    }

    public interface ListTypeSwitchedListener {
        void onListTypeSwitched(String listType);
    }

    public boolean isLeftList() {
        if ("left".equals(mParameterManager.getDvbsParaManager().getCurrentListDirection())) {
            return true;
        }
        return false;
    }

    public boolean isRightList() {
        if ("right".equals(mParameterManager.getDvbsParaManager().getCurrentListDirection())) {
            return true;
        }
        return false;
    }

    public boolean isMiddleList() {
        if ("middle".equals(mParameterManager.getDvbsParaManager().getCurrentListDirection())) {
            return true;
        }
        return false;
    }

    public boolean isSatelliteList(String type) {
        if (ITEM_SATELLITE.equals(type) && "middle".equals(mParameterManager.getDvbsParaManager().getCurrentListDirection())) {
            return true;
        }
        return false;
    }

    public boolean isTransponderList(String type) {
        if (ITEM_TRANSPONDER.equals(type) && "middle".equals(mParameterManager.getDvbsParaManager().getCurrentListDirection())) {
            return true;
        }
        return false;
    }

    public void switchListToType(String leftListType) {
        if (mListType.equals(leftListType))
            return;
        if (ITEM_SATELLITE.equals(leftListType)
                || ITEM_TRANSPONDER.equals(leftListType)
                || ITEM_LNB.equals(leftListType)) {
            mListType = leftListType;

            if (mListTypeSwitchedListener != null) {
                mListTypeSwitchedListener.onListTypeSwitched(mListType);
            }
        }
    }

    private String switchListType(String leftListType) {
        String result = null;
        if (ITEM_SATELLITE.equals(leftListType)) {
            ItemDetail item = (ItemDetail)getAdapter().getItem(getSelectedItemPosition());
            /*if (item.getEditStatus() != ItemDetail.SELECT_EDIT) {
                result = null;
            } else {
                mParameterManager.getDvbsParaManager().setCurrentSatellite(item.getFirstText());
                result = ITEM_TRANSPONDER;
            }*/
            mParameterManager.getDvbsParaManager().setCurrentSatellite(item.getFirstText());
            result = ITEM_TRANSPONDER;
        } else if (ITEM_TRANSPONDER.equals(leftListType)) {
            result = ITEM_SATELLITE;
        }
        return result;
    }
}


