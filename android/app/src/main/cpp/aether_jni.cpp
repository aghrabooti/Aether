#include <jni.h>
#include <dlfcn.h>
#include <cstdlib>
#include <string>

using StartFn = char *(*)(const char *);
using PollFn = char *(*)(uint64_t);
using CancelFn = char *(*)(uint64_t);
using FreeFn = void (*)(char *);

static void *core() { static void *h = dlopen("libaether.so", RTLD_NOW | RTLD_GLOBAL); return h; }
template<typename T> static T symbol(const char *name) { return reinterpret_cast<T>(dlsym(core(), name)); }

static std::string take(char *raw) {
    if (!raw) return "{\"ok\":false,\"error\":\"Aether native core is unavailable\"}";
    std::string result(raw);
    auto release = symbol<FreeFn>("aether_string_free");
    if (release) release(raw);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_cluvex_aether_AetherNative_start(JNIEnv *env, jobject, jstring args) {
    auto start = symbol<StartFn>("aether_core_start");
    if (!start) return 0;
    const char *value = env->GetStringUTFChars(args, nullptr);
    std::string reply = take(start(value));
    env->ReleaseStringUTFChars(args, value);
    auto pos = reply.find("\"job\":");
    if (pos == std::string::npos) return 0;
    return std::strtoll(reply.c_str() + pos + 6, nullptr, 10);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_cluvex_aether_AetherNative_poll(JNIEnv *env, jobject, jlong job) {
    auto poll = symbol<PollFn>("aether_job_poll");
    std::string reply = poll ? take(poll(job)) : "{\"ok\":false,\"error\":\"poll unavailable\"}";
    return env->NewStringUTF(reply.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_cluvex_aether_AetherNative_cancel(JNIEnv *, jobject, jlong job) {
    auto cancel = symbol<CancelFn>("aether_job_cancel");
    if (cancel) take(cancel(job));
}
