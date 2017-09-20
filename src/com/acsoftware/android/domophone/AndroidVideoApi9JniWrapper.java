package com.acsoftware.android.domophone;


import java.util.List;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
 
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class AndroidVideoApi9JniWrapper {
	static public int detectCameras(int[] indexes, int[] frontFacing, int[] orientation) {
		return 0;
	}
	

	static public int[] selectNearestResolutionAvailable(int cameraId, int requestedW, int requestedH) {
		return null;
	}
	
	public static Object startRecording(int cameraId, int width, int height, int fps, int rotation, final long nativePtr) {
	    return null;
	} 
	
	public static void stopRecording(Object cam) {

	} 
	
	public static void setPreviewDisplaySurface(Object cam, Object surf) {

	}
	
	private static void setCameraDisplayOrientation(int rotationDegrees, int cameraId, Camera camera) {
	}
	
	private static int[] findClosestEnclosingFpsRange(int expectedFps, List<int[]> fpsRanges) {
			return new int[] { 0, 0 };
	}
}
