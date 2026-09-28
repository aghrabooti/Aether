cmake_minimum_required(VERSION 3.22.1)
project(aether_jni)
add_library(aether_jni SHARED aether_jni.cpp)
target_link_libraries(aether_jni android log dl)
