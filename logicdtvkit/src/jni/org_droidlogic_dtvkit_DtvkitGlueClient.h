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

using namespace android;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;

enum {
    REQUEST = 0,
    DRAW = 1,
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

private:
    sp<DTVKitHidlClient> mDkSession;
    mutable Mutex mLock;
    static DTVKitClientJni *mInstance;
};

#endif/*__ORG_DTVKIT_INPUTSOURCE_CLIENT_H__*/

