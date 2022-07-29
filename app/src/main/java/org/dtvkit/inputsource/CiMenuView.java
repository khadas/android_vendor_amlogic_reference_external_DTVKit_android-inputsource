package com.droidlogic.dtvkit.inputsource;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.settings.PropSettingManager;
import com.droidlogic.settings.ConstantManager;

import java.util.ArrayList;

public class CiMenuView extends LinearLayout {

    private static final String TAG = "DtvkitCiMenu";
    private static final String EXIT_TO_QUIT = "Exit to quit";
    private static final int MENU_TIMEOUT_MESSAGE = 1;
    private static final int RETURN_BUTTON_NUM = 0;

    private static final int WAIT_CI_MENU_TIMEOUT = 3000;

    private boolean signalTriggered = false;
    private boolean isMenuVisible = false;
    private final Handler uiHandler;
    private final Context mContext;

    private int mCiMenuTestItemCount = 0;
    private JSONArray mCiMenuTestItemArray = null;
    private String mCiMenuTestItemType = null;
    protected final BroadcastReceiver mCiMenuItemTestBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PropSettingManager.getBoolean(PropSettingManager.CI_MENU_ITEM_TEST, false)) {
                Log.d(TAG, "TEST_CASE intent = " + (intent != null ? intent.getExtras() : null));
                if (intent != null) {
                    try {
                        String action = intent.getAction();
                        if (ConstantManager.ACTION_CI_MENU_INFO.equals(action)) {
                            String command = intent.getStringExtra(ConstantManager.COMMAND_CI_MENU);
                            switch (command) {
                                case ConstantManager.VALUE_CI_MENU_INSERT_MODULE:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_insert_module" --es module_type "MENU_LIST" --ei module_count 10
                                    mCiMenuTestItemCount = intent.getIntExtra("module_count", 0);
                                    mCiMenuTestItemType = intent.getStringExtra("module_type");
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command + ", mCiMenuTestItemCount = " + mCiMenuTestItemCount + ", mCiMenuTestItemType = " + mCiMenuTestItemType);
                                    mCiMenuTestItemArray = creatTestCiMenuItem(mCiMenuTestItemCount);
                                    clearPreviousMenu();
                                    menuCloseHandler("Ci Module is inserted", EXIT_TO_QUIT);
                                    setMenuVisible();
                                    break;
                                case ConstantManager.VALUE_CI_MENU_OPEN_MODULE:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_open_module"
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command);
                                    clearPreviousMenu();
                                    signalTriggered = true;
                                    break;
                                case ConstantManager.VALUE_CI_MENU_CLOSE_MODULE:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_close_module"
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command);
                                    clearPreviousMenu();
                                    signalTriggered = true;
                                    menuCloseHandler("Ci Module closed by HOST", EXIT_TO_QUIT);
                                    setMenuInvisible();
                                    break;
                                case ConstantManager.VALUE_CI_MENU_REMOVE_MODULE:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_remove_module"
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command);
                                    clearPreviousMenu();
                                    signalTriggered = true;
                                    menuCloseHandler("Ci Module removed", EXIT_TO_QUIT);
                                    setMenuVisible();
                                    break;
                                case ConstantManager.VALUE_CI_MENU_MMI_SCREEN_REQUEST:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_mmi_screen_request"
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command);
                                    clearPreviousMenu();
                                    signalTriggered = true;
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("bottomLine","Press OK or Exit to quit");
                                    jsonObject.put("list_num",3);
                                    jsonObject.put("subTitle", "Generic Status Reporting");
                                    jsonObject.put("title", "Application Master");
                                    JSONArray itemList = new JSONArray();
                                    itemList.put("Authentication success");
                                    itemList.put("SAC establishment success");
                                    itemList.put("SAC establishment success");
                                    jsonObject.put("itemList",itemList);
                                    Log.i(TAG,"jsonObject : " + jsonObject.toString());
                                    new CiMmiRequestMenu("CIPLUS_MMI_SCREEN_REQUEST", jsonObject);
                                    break;
                                case ConstantManager.VALUE_CI_MENU_MMI_ENQ_REQUEST:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_mmi_enq_request"
                                    Log.i(TAG, "TEST_CASE Ci Menu: command = " + command);
                                    clearPreviousMenu();
                                    signalTriggered = true;
                                    JSONObject object = new JSONObject();
                                    object.put("text", "Enter CA PIN code");
                                    object.put("textLength",4);
                                    object.put("isBlind",true);
                                    Log.i(TAG,"jsonObject : " + object.toString());
                                    new CiMmiRequestMenu("CIPLUS_MMI_ENQ_REQUEST", object);
                                    break;
                                case ConstantManager.VALUE_CIPLUS_MMI_CLOSE:
                                    //am broadcast -a ci_menu_info --es command "ci_menu_mmi_close"
                                    if (isMenuVisible) {
                                        closeCiPlusMenu();
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "mCiMenuItemTestBroadcastReceiver Exception = " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "TEST_CASE hasn't been enabled");
            }
        }
    };

    private JSONArray creatTestCiMenuItem(int count) {
        JSONArray result = new JSONArray();
        try {
            for (int i = 0; i < count; i++) {
                JSONObject obj = new JSONObject();
                obj.put("data", "test item " + (i + 1));
                result.put(obj);
            }
        } catch (Exception e) {
            Log.d(TAG, "creatTestCiMenuItem Exception = " + e.getMessage());
        }
        return result;
    }

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (signal.equals("CIPLUS_CARD_INSERT")) {
                clearPreviousMenu();
                menuCloseHandler("Ci Module is inserted", EXIT_TO_QUIT);
                setMenuVisible();
                Log.i(TAG, "Ci Menu: OnSignal " + signal);
            }
            else if (signal.equals("CIPLUS_CARD_REMOVE")) {
                Log.i(TAG, "Ci Menu: OnSignal " + signal);
                clearPreviousMenu();
                signalTriggered = true;
                menuCloseHandler("Ci Module removed", EXIT_TO_QUIT);
                setMenuVisible();
            }
            else if (signal.equals("CIPLUS_MMI_SCREEN_REQUEST")) {
                clearPreviousMenu();
                Log.i(TAG, "Ci Menu: OnSignal " + signal);
                signalTriggered = true;
                new CiMmiRequestMenu(signal, data);
            }
            else if (signal.equals("CIPLUS_MMI_ENQ_REQUEST")) {
                Log.i(TAG, "Ci Menu: OnSignal " + signal);
                clearPreviousMenu();
                signalTriggered = true;
                new CiMmiRequestMenu(signal, data);
            } else if (signal.equals("CIPLUS_MMI_CLOSE")) {
                if (isMenuVisible) {
                    closeCiPlusMenu();
                }
            }
        }
    };

    private class TimerHandler extends Handler {
        public void handleMessage(Message msg) {
            if (msg.what == MENU_TIMEOUT_MESSAGE && signalTriggered == false) {
                setMenuTitleText("Ci menu response timeout. Check CAM is inserted");
                setMenuSubTitleText("", false);
                setDivideLineVisible(false);
                setMenuFooterText(EXIT_TO_QUIT);
            }
        }
    }

    private static class MenuItem {
        private String itemText;
        private int itemNum;

        public MenuItem(int itemNum, String itemText) {
            this.itemNum = itemNum;
            this.itemText = itemText;
        }

        public String getItemText() {
            return itemText;
        }

        public int getItemNum() {
            return itemNum;
        }
    }

    private class CiMmiRequestMenu {
        public CiMmiRequestMenu(String CiMenuType, JSONObject data) {
            setMenuVisible();
            if ("CIPLUS_MMI_SCREEN_REQUEST".equals(CiMenuType)) {
                initScreenRequestView(data);
            } else if ("CIPLUS_MMI_ENQ_REQUEST".equals(CiMenuType)) {
                initEnqRequestView(data);
            }
        }
        private void initScreenRequestView(JSONObject object) {
            JSONArray data = null;
            String bottomLine = "";
            int listNum = 0;
            String subTitle = "";
            String title = "";
            try {
                bottomLine = object.getString("bottomLine");
                listNum = object.getInt("list_num");
                subTitle = object.getString("subTitle");
                title = object.getString("title");
                data = object.optJSONArray("itemList");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (data == null) {
                    data = new JSONArray();
                }
            }
            if (!TextUtils.isEmpty(title)) {
                setMenuTitleText(title);
            }

            if (!TextUtils.isEmpty(subTitle)) {
                setMenuSubTitleText(subTitle, true);
            }

            setDivideLineVisible(true);

            if (!TextUtils.isEmpty(bottomLine)) {
                setMenuFooterText(bottomLine);
            }
            ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
            for (int i = 0 ; i < data.length() ; i ++) {
                try {
                    String itemText;
                    itemText = (String) data.get(i);
                    Log.i(TAG,"itemText : " + itemText);
                    menuItems.add(new MenuItem(i + 1, itemText));
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            for (int i = 0; i < menuItems.size(); i++) {
                setUpAndPrintMenuItem(menuItems.get(i).getItemNum(), menuItems.get(i).getItemText());
            }

            /* Set focus on the first menu item */
            setMenuFocus(1, true);
        }

        private void initEnqRequestView(JSONObject object) {
            String title = "";
            int textLength = 0;
            boolean isBlind = false;
            try{
                title = object.getString("text") ;
                textLength = object.getInt("textLength");
                isBlind = object.getBoolean("isBlind");
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
            }
            setMenuTitleText("");
            setDivideLineVisible(false);
            setMenuSubTitleText("",false);
            setMenuFooterText(" ");
            createEnquiryMenu(title, textLength, isBlind);
        }

        private void setUpAndPrintMenuItem(final int buttonNum, final String itemText) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);
                    final LayoutInflater inflater = LayoutInflater.from(mContext);
                    final TextView textView = (TextView)inflater.inflate(R.layout.mmi_button, null);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(findViewById(R.id.textViewMenuTitle).getWidth(), 40));
                    textView.setText(itemText);
                    textView.setId(buttonNum);

                    textView.setFocusable(true);
                    textView.setFocusableInTouchMode(true);

                    linearLayout.addView(textView);

                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectMenuOption(textView.getId());
                            Log.i(TAG, "Clicking button " + Integer.toString(textView.getId()));
                        }
                    });

                    textView.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            /* Return to previous level */
                            if (keyCode == KeyEvent.KEYCODE_CLEAR && KeyEvent.ACTION_UP == event.getAction()) {
                                Log.i(TAG,"Return to previous level");
                                selectMenuOption(RETURN_BUTTON_NUM);
                            }
                            return false;
                        }
                    });
                }
            });
        }

        private void selectMenuOption(final int optionId) {
            JSONArray args = new JSONArray();
            args.put(optionId);

            try {
                JSONObject obj = DtvkitGlueClient.getInstance().request("CIPlus.setMenuAnswer", args);
            } catch (Exception ignore) {
                Log.e(TAG, ignore.getMessage());
            }
        }

        private void createEnquiryMenu(final String text, final int maxLength, final boolean hide) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);

            final LinearLayout parentLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);
            final LinearLayout enquiryLayout = (LinearLayout)inflater.inflate(R.layout.enquiry_form, null);

            final TextView enquiryTextView = (TextView)(enquiryLayout.findViewById(R.id.textViewEnquiryName));
            final EditText enquiryEditText = (EditText)(enquiryLayout.findViewById(R.id.editTextEnquiry));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enquiryLayout.setId(0);
                    enquiryTextView.setText(text);
                    enquiryEditText.requestFocus();

                    enquiryEditText.setFilters(new InputFilter[] {
                            new InputFilter.LengthFilter(maxLength)
                    });

                    if (hide) {
                        enquiryEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }

                    parentLayout.addView(enquiryLayout);

                    enquiryEditText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sendEnquiryResponse(enquiryEditText.getText().toString() );
                        }
                    });
                }
            });
        }

        private void sendEnquiryResponse(final String response) {
            JSONArray args = new JSONArray();
            args.put(response);
            args.put("data");
            try {
                JSONObject obj = DtvkitGlueClient.getInstance().request("CIPlus.setEnqAnswer", args);
            } catch (Exception ignore) {
                Log.e(TAG, ignore.getMessage());
            }
        }
    }


    public CiMenuView(Context context) {
        super(context);

        Log.i(TAG, "onCreateCiMenuView");

        uiHandler = new Handler(context.getMainLooper());
        mContext = context;

        setMenuInvisible();
        inflate(getContext(), R.layout.ci_menu,  this);

        startListeningForCamSignal();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConstantManager.ACTION_CI_MENU_INFO);
        mContext.registerReceiver(mCiMenuItemTestBroadcastReceiver, intentFilter);
    }

    public void destroy() {
        Log.i(TAG, "destroy and unregisterSignalHandler");
        stopListeningForCamSignal();
        mContext.unregisterReceiver(mCiMenuItemTestBroadcastReceiver);
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        boolean used;

        /* We action the Launch button regardless of if the menu is open */
        if (keyCode == KeyEvent.KEYCODE_NUM_LOCK) {
            used = true;
        }
        else {
            used = handleGenericKeypress(event);
        }

        Log.i(TAG, "Ci Menu Key down, used: " + Boolean.toString(used));

        return used;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        boolean used;

        switch (keyCode) {
            case KeyEvent.KEYCODE_NUM_LOCK:
                used = handleKeypressLaunch();
                break;
            case KeyEvent.KEYCODE_BACK:
                used = handleKeypressExit();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                used = handleKeypressUp();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                used = handleKeypressDown();
                break;
            default:
                used = handleGenericKeypress(event);
                break;
        }
        Log.i(TAG, "Ci Menu Key up, used: " + Boolean.toString(used));

        return used;
    }

    private boolean handleKeypressLaunch() {
        launchCiMenu();
        return true;
    }

    private boolean handleKeypressExit() {
        boolean used = false;

        if (isMenuVisible) {
            closeCiPlusMenu();
            used = true;
        }
        return used;
    }

    private boolean handleKeypressUp() {
        int currentFocus;
        boolean used = false;

        if (isMenuVisible) {
            currentFocus = getCurrentFocusedItem();
            if (currentFocus > 1) {
                setMenuFocus(currentFocus - 1, true);
            }
            used = true;
        }
        return used;
    }

    private boolean handleKeypressDown() {
        int currentFocus;
        boolean used = false;

        if (isMenuVisible) {
            currentFocus = getCurrentFocusedItem();
            if (currentFocus < numberOfMenuItems()) {
                setMenuFocus(currentFocus + 1, false);
            }
            used = true;
        }
        return used;
    }

    private boolean handleGenericKeypress(KeyEvent event) {
        LinearLayout mmiItems;
        View currentFocusedItem;
        boolean used = false;

        if (isMenuVisible) {
            mmiItems = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);
            currentFocusedItem = mmiItems.getFocusedChild();

            if (currentFocusedItem != null) {
                currentFocusedItem.dispatchKeyEvent(event);
            }
            used = true;
        }
        return used;
    }

    private boolean enterCiPlusMenu() {
        boolean result = false;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("CIPlus.enterMMI", new JSONArray());
            result =  obj.getBoolean("data");

        } catch (Exception ignore) {
            Log.e(TAG, ignore.getMessage());
        }

        return result;
    }

    private void launchCiMenu() {
        TimerHandler timerHandler = new TimerHandler();

        setMenuVisible();

        if (enterCiPlusMenu() == false) {
            setMenuTitleText("Ci Module not detected");
            setDivideLineVisible(false);
            setMenuSubTitleText("",false);
            setMenuFooterText(EXIT_TO_QUIT);
            Log.e(TAG, "Ci Module not detected");
        }
        else {
            /* Although enterCiMenu returns TRUE, there are no guarantees that the menu has been
               entered. We must wait for a signal before telling the user that a menu has not
               been found */
            timerHandler.sendEmptyMessageDelayed(MENU_TIMEOUT_MESSAGE, WAIT_CI_MENU_TIMEOUT);
        }
    }

    private void closeCiPlusMenu() {
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("CIPlus.closeMMI", new JSONArray());
        } catch (Exception ignore) {
            Log.e(TAG, ignore.getMessage());
        }

        clearPreviousMenu();
        setMenuInvisible();
    }

    private void startListeningForCamSignal() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler, DtvkitGlueClient.INDEX_FOR_MAIN);
    }

    private void stopListeningForCamSignal() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
    }

    private void menuCloseHandler(final String titleText, final String footerText) {
        // if (isMenuVisible) {
            setMenuTitleText(titleText);
            setMenuFooterText(footerText);
            setMenuSubTitleText("",false);
            setDivideLineVisible(false);
        // }
    }

    private void setMenuFocus(final int buttonNum, final boolean up) {
        final ScrollView scrollView = (ScrollView)findViewById(R.id.scrollContainer);
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView button = (TextView)linearLayout.findViewById(buttonNum);

                if (linearLayout.getChildCount() > 0) {
                    button.setFocusable(true);
                    button.setFocusableInTouchMode(true);
                    button.requestFocus();
                    scrollView.smoothScrollBy(0, up ? -button.getMeasuredHeight() : button.getMeasuredHeight());
                }
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        uiHandler.post(r);
    }

    private void setMenuVisible() {
        final CiMenuView runnableView = this;
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI);
        Log.i(TAG, "set MMI Menu Visible");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnableView.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                runnableView.requestFocus();
            }
        });
        isMenuVisible = true;
    }

    private void setMenuInvisible() {
        final CiMenuView runnableView = this;

        Log.i(TAG, "set MMI Menu Invisible");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnableView.setVisibility(View.INVISIBLE);
            }
        });
        isMenuVisible = false;
    }

    private void clearPreviousMenu() {
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayout.removeAllViews();
            }
        });
    }

    private int getCurrentFocusedItem() {
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);
        final View currentFocusChild = linearLayout.getFocusedChild();
        int focusedId = 0;

        if (currentFocusChild != null) {
            focusedId = currentFocusChild.getId();
        }

        return focusedId;
    }

    private int numberOfMenuItems() {
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.ll_ci_MMI_Items);
        return linearLayout.getChildCount();
    }

    private void setMenuTitleText(final String text) {
        Log.i(TAG, "setMenuTitleText: text = " + text);
        final TextView textMenuTitle = (TextView)findViewById(R.id.textViewMenuTitle);
        printReceivedSignal(text, textMenuTitle);
    }


    private void setMenuSubTitleText(final String text, boolean isMenuVisible) {
        Log.i(TAG, "setMenuSubTitleText: text = " + text);
        final TextView textMenuSubTitle = (TextView)findViewById(R.id.textViewMenuSubTitle);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textMenuSubTitle.setVisibility(isMenuVisible ? View.VISIBLE : View.GONE);
            }
        });
        printReceivedSignal(text, textMenuSubTitle);
    }


    private void setMenuFooterText(final String text) {
        Log.i(TAG, "setMenuFooterText: text = " + text);
        final TextView textMenuFooter = (TextView)findViewById(R.id.textViewMenuFooter);
        printReceivedSignal(text, textMenuFooter);
    }

    private void setDivideLineVisible(boolean isDivideLineVisible) {
        final View divideTopLine = (View)findViewById(R.id.ci_menu_top_line);
        final View divideBottomLine = (View)findViewById(R.id.ci_menu_bottom_line);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                divideTopLine.setVisibility(isDivideLineVisible ? View.VISIBLE : View.GONE);
                divideBottomLine.setVisibility(isDivideLineVisible ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void printReceivedSignal(final String sigText, final TextView text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Entered print received signal with text: " + sigText);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                text.setText(sigText);
            }
        });
    }
}
