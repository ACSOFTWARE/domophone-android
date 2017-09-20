#!/bin/sh


cp ../../../linphone-android/libs/armeabi-v7a/libffmpeg-arm.so ../libs/armeabi-v7a/libffmpeg-arm.so
cp ../../../linphone-android/libs/armeabi-v7a/liblinphone-armeabi-v7a.so ../libs/armeabi-v7a/liblp.so

cp ../../../linphone-android/libs/armeabi-v7a/libffmpeg-arm.so ../libs/armeabi/libffmpeg-arm.so
cp ../../../linphone-android/libs/armeabi/liblinphone-armeabi.so ../libs/armeabi/liblp.so

cp ../../../linphone-android/libs/x86/libffmpeg-x86.so ../libs/x86/libffmpeg-arm.so
cp ../../../linphone-android/libs/x86/liblinphone-x86.so ../libs/x86/liblp.so

rm -f ../libs/armeabi-v7a/gdb.setup
rm -f ../libs/armeabi-v7a/gdbserver

rm -f ../libs/armeabi/gdb.setup
rm -f ../libs/armeabi/gdbserver

rm -f ../libs/x86/gdb.setup
rm -f ../libs/x86/gdbserver
