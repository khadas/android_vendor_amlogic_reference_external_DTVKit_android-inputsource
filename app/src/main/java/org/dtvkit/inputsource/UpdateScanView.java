package com.droidlogic.dtvkit.inputsource;

import android.content.Intent;
import android.os.Bundle;

public interface UpdateScanView {
        default void updateScanProgress(int progress){}
        default void updateScanChannelNumber(int digitalChannelNumber, int ipChannelNumber){}
        default void updateScanSignalStrength(int signalStrength){}
        default void updateScanSignalQuality(int signalQuality){}
        default void updateScanStatus(String status){}
        default void finishScanView(){}
        default void notifyUpdateViewEvent(String evneType, Bundle event){}
}
