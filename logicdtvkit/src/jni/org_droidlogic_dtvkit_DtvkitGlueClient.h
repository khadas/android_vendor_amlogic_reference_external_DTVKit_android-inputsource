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
    SUB_SERVER_DRAW = 2,
    HBBTV_DRAW     = 3,
};

enum {
    SUBTITLE_START = 0,
    SUBTITLE_STOP,
    SUBTITLE_PAUSE,
    SUBTITLE_RESUME,
    TELETEXT_EVENT,
    SUBTITLE_TUNE,
};

enum {
    SUBTITLE_CTL_ATTACH = 100,
    SUBTITLE_CTL_DETTACH,
    SUBTITLE_CTL_DESTROY,
    SUBTITLE_CTL_OPEN_USERDATA,
    SUBTITLE_CTL_CLOSE_USERDATA,
    SUBTITLE_CTL_SET_REGION_ID,
    SUBTITLE_CTL_RESET_FOR_SEEK,
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

typedef struct dvb_param_s {
    std::string resource;
    std::string json;
    int id;
}dvb_param_t;

class DTVKitClientJni : public DTVKitListener {
public:
    DTVKitClientJni();
    ~DTVKitClientJni();
    virtual void notify(const parcel_t &parcel);
    virtual void notifyServerState(int diedOrReconnected);

    static DTVKitClientJni *GetInstance();
    std::string request(const std::string& resource, const std::string& json);
    void setAfd(int player, int afd);
    void setSubtitleFlag(int flag);
    MessageQueueSync* getQueue();

private:
    static void  once_run(void);
    static void* pid_run(void *arg);
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
        void onSubtitleAfdEvent(int dec_id, int afd);
        void onSubtitleDimension(int width, int height) {}
        void onSubtitleLanguage(std::string lang) {};
        void onSubtitleInfo(int what, int extra) {};
        void onMixVideoEvent(int val);
        virtual void onServerDied();
        void onSubtitleUIEvent(int uiCmd, const std::vector<int> &params) {}
};

class SubtitleMessageHandler : public android::MessageHandler {
    public:
        SubtitleMessageHandler() {}
        void setParcelData(parcel_t parcel);
        void setParam(int    param);
    protected:
        virtual ~SubtitleMessageHandler() {}
    private:
        void handleMessage(const Message& message);
        parcel_t parcel;
        int param;
};

struct SubtitleLooperThread : public Thread {
public:
    SubtitleLooperThread(sp<Looper> looper)
        : mLooper(looper) {
    }

    virtual bool threadLoop() {
        if(mLooper.get() == nullptr)
            return false;
        int32_t ret = mLooper->pollOnce(-1);
        return true;
    }

protected:
    virtual ~SubtitleLooperThread() {}

private:
    sp<Looper> mLooper;
};

#endif/*__ORG_DTVKIT_INPUTSOURCE_CLIENT_H__*/

