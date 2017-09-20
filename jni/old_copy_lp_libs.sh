#!/bin/sh


SRC_DIR="../../../linphone-android/libs/armeabi-v7a"
DST_DIR="../libs/armeabi-v7a"
LP=lp

cp $SRC_DIR/libavcodec-linphone-arm.so $DST_DIR/libavcodec-$LP-arm.so 
cp $SRC_DIR/libavutil-linphone-arm.so $DST_DIR/libavutil-$LP-arm.so 
cp $SRC_DIR/liblinphone-armeabi-v7a.so $DST_DIR/liblp-armeabi-v7a.so
cp $SRC_DIR/libswscale-linphone-arm.so $DST_DIR/libswscale-$LP-arm.so
cp $SRC_DIR/libffmpeg-lp-arm.so $DST_DIR/libffmpeg-lp-arm.so


SRC_DIR="../../../linphone-android/libs/armeabi"
DST_DIR="../libs/armeabi"

cp $SRC_DIR/liblinphone-armeabi.so $DST_DIR/lib$LP-armeabi.so

SRC_DIR="../../../linphone-android/libs/x86"
DST_DIR="../libs/x86"

cp $SRC_DIR/libavcodec-linphone-x86.so $DST_DIR/libavcodec-$LP-x86.so
cp $SRC_DIR/libavutil-linphone-x86.so $DST_DIR/libavutil-$LP-x86.so
cp $SRC_DIR/liblinphone-x86.so $DST_DIR/lib$LP-x86.so
cp $SRC_DIR/libswscale-linphone-x86.so $DST_DIR/libswscale-$LP-x86.so
