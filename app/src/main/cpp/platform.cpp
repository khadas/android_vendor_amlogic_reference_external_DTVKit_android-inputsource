#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <string.h>

#ifdef PLATFORM_BROADCOM
#include <cutils/native_handle.h>
#include <bcmsideband.h>
#include <bcmsidebandplayerfactory.h>
static struct bcmsideband_ctx *context = NULL;
#endif

#ifdef PLATFORM_AMLOGIC
#define LOG_TAG "SurfaceOverlay-jni"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <jni.h>
#include <libnativehelper/JNIHelp.h>

#include <log/log.h>
#include <utils/KeyedVector.h>
#include <android_runtime/AndroidRuntime.h>
#include <android_runtime/android_view_Surface.h>
#include <android/native_window.h>
#include <gui/Surface.h>
#include <gui/IGraphicBufferProducer.h>
#include <ui/GraphicBuffer.h>
#include <amlogic/am_gralloc_ext.h>
#endif

static jboolean set = JNI_FALSE;
static int surface_x = 0;
static int surface_y = 0;
static int surface_width = 1920;
static int surface_height = 1080;

#ifdef PLATFORM_BROADCOM
static void onRectangleUpdated(void *context, unsigned int x, unsigned int y, unsigned int width, unsigned int height)
{
   __android_log_print(ANDROID_LOG_INFO, "DTVKitSource", "onRectangleUpdated: x %d, y %d, width %d, height %d\n",
      x, y, width, height);
   surface_x = x;
   surface_y = y;
   surface_width = width;
   surface_height = height;
}
#endif

#ifdef PLATFORM_AMLOGIC
namespace android
{
    static int updateNative(sp<ANativeWindow> nativeWin) {
        char* vaddr;
        int ret = 0;
        ANativeWindowBuffer* buf;

        if (nativeWin.get() == NULL) {
            return 0;
        }
        int err = nativeWin->dequeueBuffer_DEPRECATED(nativeWin.get(), &buf);
        if (err != 0) {
            ALOGE("dequeueBuffer failed: %s (%d)", strerror(-err), -err);
            return -1;
        }
        /*
        nativeWin->lockBuffer_DEPRECATED(nativeWin.get(), buf);
        sp<GraphicBuffer> graphicBuffer(new GraphicBuffer(buf, false));
        graphicBuffer->lock(1, (void **)&vaddr);
        if (vaddr != NULL) {
            memset(vaddr, 0x0, graphicBuffer->getWidth() * graphicBuffer->getHeight() * 4); //to show video in osd hole...
        }
        graphicBuffer->unlock();
        graphicBuffer.clear();*/

        return nativeWin->queueBuffer_DEPRECATED(nativeWin.get(), buf);
    }

    native_handle_t *pTvStream = nullptr;
    sp<Surface> mSurface;
    sp<NativeHandle> mSourceHandle;
    static int getTvStream() {
        int ret = -1;
        if (pTvStream == nullptr) {
            pTvStream = am_gralloc_create_sideband_handle(AM_TV_SIDEBAND, 1);
            if (pTvStream == nullptr) {
                ALOGE("tvstream can not be initialized");
                return ret;
            }
        }
        return 0;
    }

    static void setSurface(JNIEnv *env, jclass thiz __unused, jobject jsurface) {
        ALOGD("SetSurface");
        sp<Surface> surface;
        if (jsurface) {
            surface = android_view_Surface_getSurface(env, jsurface);

            if (surface == NULL) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                                  "The surface has been released");
                return;
            }
        }

        if (mSurface == surface) {
            // Nothing to do
            return;
        }

        if (mSurface != NULL) {
            if (Surface::isValid(mSurface)) {
                mSurface->setSidebandStream(NULL);
            }
            mSurface.clear();
        }

        if (getTvStream() == 0) {
            mSourceHandle = NativeHandle::create(pTvStream, false);
            mSurface = surface;
            if (mSurface != nullptr) {
                mSurface->setSidebandStream(mSourceHandle);
            }
        }
        return;
    }
}
using namespace android;

#endif

extern "C" JNIEXPORT jboolean JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_setNativeSurface(JNIEnv *env, jclass thiz, jobject surface)
{
   if (set != JNI_TRUE)
   {
      ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
      #ifdef PLATFORM_BROADCOM
      __android_log_print(ANDROID_LOG_INFO, "DTVKitSource", "setNativeSurface: Broadcom platform selected (width %d, height %d)\n",
         ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
      int index = 0, videoId = -1, audioId = -1, surfaceId = -1;
      if ((context = libbcmsideband_init_sideband(index, window, &videoId, &audioId, &surfaceId, &onRectangleUpdated)))
      {
         set = JNI_TRUE;
      }
      #elif defined (PLATFORM_AMLOGIC)

      setSurface(env, thiz, surface);

      #else
      __android_log_print(ANDROID_LOG_WARN, "DTVKitSource", "setNativeSurface: no platform selected (width %d, height %d)\n",
         ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
      ANativeWindow_setBuffersGeometry(window, surface_width, surface_height, WINDOW_FORMAT_RGBA_8888);
      ANativeWindow_Buffer buffer;
      if (ANativeWindow_lock(window, &buffer, 0) == 0)
      {
         memset(buffer.bits, 0x00, surface_width * surface_height * 4);
         ANativeWindow_unlockAndPost(window);
      }
      set = JNI_TRUE;
      #endif
   }

   return set;
}

extern "C" JNIEXPORT void JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_unsetNativeSurface(JNIEnv* env, jclass thiz) {
   if (set == JNI_TRUE)
   {
      #ifdef PLATFORM_BROADCOM
      libbcmsideband_release(context);
      context = NULL;
      #endif
      set = JNI_FALSE;
   }
}

extern "C" JNIEXPORT jint JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_getNativeSurfaceX(JNIEnv *env, jobject instance) {
   return surface_x;
}

extern "C" JNIEXPORT jint JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_getNativeSurfaceY(JNIEnv *env, jobject instance) {
   return surface_y;
}

extern "C" JNIEXPORT jint JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_getNativeSurfaceWidth(JNIEnv *env, jobject instance) {
   return surface_width;
}

extern "C" JNIEXPORT jint JNICALL Java_com.droidlogic.dtvkit_inputsource_Platform_getNativeSurfaceHeight(JNIEnv *env, jobject instance) {
   return surface_height;
}
