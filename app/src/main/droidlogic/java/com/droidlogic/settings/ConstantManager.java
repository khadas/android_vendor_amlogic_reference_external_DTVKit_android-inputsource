package com.droidlogic.settings;

import android.util.Log;
import android.media.tv.TvTrackInfo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import com.droidlogic.app.SystemControlManager;

public class ConstantManager {

    private static final String TAG = "ConstantManager";
    private static final boolean DEBUG = true;

    public static final String PI_FORMAT_KEY = "pi_format";
    public static final String KEY_AUDIO_CODES_DES = "audio_codes";
    public static final String KEY_TRACK_PID = "pid";
    public static final String KEY_INFO = "info";

    public static final String EVENT_STREAM_PI_FORMAT = "event_pi_format";
    public static final String EVENT_RESOURCE_BUSY = "event_resource_busy";

    //show or hide overlay
    public static final String ACTION_TIF_CONTROL_OVERLAY = "tif_control_overlay";
    public static final String KEY_TIF_OVERLAY_SHOW_STATUS = "tif_overlay_show_status";
    //surface related
    public static final String KEY_SURFACE = "surface";
    public static final String KEY_TV_STREAM_CONFIG = "config";

    public static final String CONSTANT_QAA = "qaa";//Original Audio flag
    public static final String CONSTANT_ORIGINAL_AUDIO = "Original Audio";
    public static final String CONSTANT_UND_FLAG = "und";//undefined flag
    public static final String CONSTANT_UND_VALUE = "Undefined";

    public static final Map<String, String> PI_TO_VIDEO_FORMAT_MAP = new HashMap<>();
    static {
        PI_TO_VIDEO_FORMAT_MAP.put("interlace", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("progressive", "P");
        PI_TO_VIDEO_FORMAT_MAP.put("interlace-top", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("interlace-bottom", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("Compressed", "P");
    }

    public static final String CONSTANT_FORMAT_PROGRESSIVE = "progressive";
    public static final String CONSTANT_FORMAT_INTERLACE = "interlace";
    public static final String CONSTANT_FORMAT_COMRPESSED = "Compressed";

    public static final String SYS_HEIGHT_PATH = "/sys/class/video/frame_height";
    public static final String SYS_PI_PATH = "/sys/class/deinterlace/di0/frame_format";//"/sys/class/video/frame_format";

	//add define for subtitle type
	public static final int ADB_SUBTITLE_TYPE_DVB  = 0x10;
	public static final int ADB_SUBTITLE_TYPE_DVB_4_3 = 0x11;
	public static final int ADB_SUBTITLE_TYPE_DVB_16_9 = 0x12;
	public static final int ADB_SUBTITLE_TYPE_DVB_221_1 = 0x13;
	public static final int ADB_SUBTITLE_TYPE_DVB_HD = 0x14;
	public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING = 0x20;
	public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_4_3 = 0x21;
	public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_16_9 = 0x22;
	public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_221_1 = 0x23;
	public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD = 0x24;

	//add define for telextet type
	public static final int ADB_TELETEXT_TYPE_INITIAL = 0x01;
	public static final int ADB_TELETEXT_TYPE_SUBTITLE = 0x02;
	public static final int ADB_TELETEXT_TYPE_ADDITIONAL_INFO = 0x03;
	public static final int ADB_TELETEXT_TYPE_SCHEDULE = 0x04;
	public static final int ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING = 0x05;

    public static void ascendTrackInfoOderByPid(List<TvTrackInfo> list) {
        if (list != null) {
            Collections.sort(list, new PidAscendComparator());
        }
    }

    public static class PidAscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid1.compareTo(pid2);
        }
    }

    public static class PidDscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid2.compareTo(pid1);
        }
    }
}
