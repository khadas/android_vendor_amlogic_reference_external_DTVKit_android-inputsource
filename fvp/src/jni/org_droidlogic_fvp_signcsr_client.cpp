/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#define LOG_TAG "Fvp-jni"

#include <sys/prctl.h>
#include <sys/ioctl.h>

#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

extern "C" {
#include <tee_client_api.h>
}

#include "org_droidlogic_fvp_signcsr_client.h"

#define SIGNED_CSR_LEN     4096
#define TA_CMD_RSA_SIGN_TEST   0xFFFF9005
#define TA_FVP_UUID { 0x5f440c5c, 0x87cc, 0x4e97, { 0x96, 0x32, 0x74, 0xe4, 0x0a, 0x19, 0x4a, 0x1f } }


static JavaVM   *gJavaVM = NULL;

static jbyteArray signCSR(JNIEnv *env, jobject thiz, jbyteArray jcsr, jbyteArray jPublicKey) {
    ALOGI("signCSR in");

    TEEC_Result res;
    TEEC_Context ctx;
    TEEC_Session sess;
    TEEC_Operation op;
    TEEC_UUID uuid = TA_FVP_UUID;
    uint32_t err_origin;

    char *signed_csr = NULL;
    jbyte* csr = NULL;
    jsize  csr_len = 0;
    jbyte* public_key = NULL;
    jsize  public_key_len = 0;
    jbyteArray array = NULL;

    //1.init comtext
    res = TEEC_InitializeContext(NULL, &ctx);
    if (res != TEEC_SUCCESS) {
        ALOGD("TEEC_InitializeContext ret 0x%X\n", res);
        return NULL;
    }

    //2.open TA secssion
    res = TEEC_OpenSession(&ctx, &sess, &uuid, TEEC_LOGIN_PUBLIC, NULL, NULL, &err_origin);
    if (res != TEEC_SUCCESS) {
        ALOGD("TEEC_Opensession ret 0x%X origin 0x%X\n",res, err_origin);
        TEEC_FinalizeContext(&ctx);
        return NULL;
    }

    memset(&op, 0, sizeof(TEEC_Operation));
    op.paramTypes = TEEC_PARAM_TYPES(TEEC_MEMREF_TEMP_INPUT, TEEC_MEMREF_TEMP_INPUT, \
        TEEC_MEMREF_TEMP_OUTPUT, TEEC_NONE);

    signed_csr = (char*)malloc(SIGNED_CSR_LEN);
    if (signed_csr == NULL) {
        ALOGD("signCSR malloc error");
        TEEC_FinalizeContext(&ctx);
        return NULL;
    }

    //3.get csr and public key
    if (jcsr != NULL) {
        csr = env->GetByteArrayElements(jcsr, NULL);
        csr_len = env->GetArrayLength(jcsr);
    }

    if (jPublicKey != NULL) {
        public_key = env->GetByteArrayElements(jPublicKey, NULL);
        public_key_len = env->GetArrayLength(jPublicKey);
    }
    ALOGD("csr_len = %d, public_key_len = %d", csr_len, public_key_len);

    op.params[0].tmpref.buffer = (void*)csr;
    op.params[0].tmpref.size = csr_len;
    op.params[1].tmpref.buffer = (void*)public_key;
    op.params[1].tmpref.size = public_key_len;
    op.params[2].tmpref.buffer = signed_csr;
    op.params[2].tmpref.size = SIGNED_CSR_LEN; // should >= 4096

    //4.command
    res = TEEC_InvokeCommand(&sess, TA_CMD_RSA_SIGN_TEST, &op, &err_origin);
    if (res != TEEC_SUCCESS) {
        ALOGD("sign csr failed , res = 0x%x\r\n", res);
    }

    array = env->NewByteArray(sizeof(signed_csr));
    if (array != NULL) {
        env->SetByteArrayRegion(array, 0, sizeof(signed_csr), (const jbyte*)signed_csr);
    }
    ALOGD("signed_csr_len = %d", sizeof(signed_csr));

    //5.release resource
    TEEC_CloseSession(&sess);
    TEEC_FinalizeContext(&ctx);

    if (csr != NULL) {
        env->ReleaseByteArrayElements(jcsr, csr, 0);
    }

    if (public_key != NULL) {
        env->ReleaseByteArrayElements(jPublicKey, public_key, 0);
    }

    if (signed_csr != NULL) {
       free(signed_csr);
    }

    ALOGI("signCSR out");
    return array;
}

static JNINativeMethod gMethods[] = {
{
    "native_signCSR", "([B[B)[B",
    (void *)signCSR
},
};

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_METHOD_ID(var, clazz, methodName, methodDescriptor) \
        var = env->GetMethodID(clazz, methodName, methodDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method " methodName);

int register_org_droidlogic_fvp_signcsr_client(JNIEnv *env)
{
    static const char *const kClassPathName = "org/droidlogic/fvp/FvpInterface";
    jclass clazz;
    int rc;
    FIND_CLASS(clazz, kClassPathName);

    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'\n", kClassPathName);
        return -1;
    }

    rc = (env->RegisterNatives(clazz, gMethods, NELEM(gMethods)));
    if (rc < 0) {
        env->DeleteLocalRef(clazz);
        ALOGE("RegisterNatives failed for '%s' %d\n", kClassPathName, rc);
        return -1;
    }

    return rc;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved __unused)
{
    JNIEnv *env = NULL;
    jint result = -1;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        ALOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);
    gJavaVM = vm;
    if (register_org_droidlogic_fvp_signcsr_client(env) < 0)
    {
        ALOGE("Can't register fvp_signcsr_client");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}


