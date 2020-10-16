/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __ORG_DTVKIT_INPUTSOURCE_CLIENT_H__
#define __ORG_DTVKIT_INPUTSOURCE_CLIENT_H__
#include <jni.h>
#include <utils/Log.h>
#include "DTVKitHidlClient.h"
#include "SubtitleServerClient.h"
using namespace android;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using android::Mutex;
using amlogic::SubtitleServerClient;
using amlogic::SubtitleListener;

enum {
    REQUEST        = 0,
    DTVKIT_DRAW    = 1,
    SUBSERVER_DRAW = 2
};

enum {
    SUBTITLE_START = 0,
    SUBTITLE_STOP,
    SUBTITLE_PAUSE,
    SUBTITLE_RESUME,
    TELETEXT_EVENT
};

typedef struct datablock_s {
    int width;
    int height;
    int dst_x;
    int dst_y;
    int dst_width;
    int dst_height;
    hidl_memory mem;
} datablock_t;

typedef struct dvbparam_s {
    std::string resource;
    std::string json;
}dvbparam_t;

class DTVKitClientJni : public DTVKitListener {
public:
    DTVKitClientJni();
    ~DTVKitClientJni();
    virtual void notify(const parcel_t &parcel);

    static DTVKitClientJni *GetInstance();
    std::string request(const std::string& resource, const std::string& json);
    void setAfd(int afd);
    void setSubtitleFlag(int flag);

private:
    sp<DTVKitHidlClient> mDkSession;
    mutable Mutex mLock;
    static DTVKitClientJni *mInstance;
};

class SubtitleDataListenerImpl : public SubtitleListener {
    public:
        SubtitleDataListenerImpl() {}
        ~SubtitleDataListenerImpl() {}

        virtual void onSubtitleEvent(const char *data, int size, int parserType,
                int x, int y, int width, int height,
                int videoWidth, int videoHeight, int cmd);
        virtual void onSubtitleDataEvent(int event, int id) {}
        void onSubtitleAvail(int avail) {};
        void onSubtitleAfdEvent(int afd);
        void onSubtitleDimension(int width, int height) {}
        void onSubtitleLanguage(std::string lang) {};
        void onSubtitleInfo(int what, int extra) {};
        void onMixVideoEvent(int val);
        virtual void onServerDied();
        void onSubtitleUIEvent(int uiCmd, const std::vector<int> &params) {}
};
#endif/*__ORG_DTVKIT_INPUTSOURCE_CLIENT_H__*/

