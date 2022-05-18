package com.droidlogic.dtvkit.inputsource.searchguide;

import org.json.JSONObject;

public interface OnMessageHandler {
    void handleMessage(String signal, JSONObject data);
}
