#!/bin/sh
env |grep ndk-r10c||export PATH=$PATH:/Users/AC/Devel/projekty/Android/ndk-r10c
env |grep sdk/tools||export PATH=$PATH:/Users/AC/Devel/projekty/Android/sdk/tools
env |grep sdk/platform-tools||export PATH=$PATH:/Users/AC/Devel/projekty/Android/sdk/platform-tools
_PWD=$(pwd)
cd ../../../linphone-android/
make liblinphone-android-sdk
cd $_PWD
./copy_lp_libs.sh
