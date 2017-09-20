package com.acsoftware.android.domophone;

import android.hardware.Camera;
 
public class AndroidVideoApi5JniWrapper {
	public static boolean isRecording = false;
	
	static public native void setAndroidSdkVersion(int version);
	public static native void putImage(long nativePtr, byte[] buffer);
	
	static public int detectCameras(int[] indexes, int[] frontFacing, int[] orientation) {
		return 0;
	}
	
	static public int[] selectNearestResolutionAvailable(int cameraId, int requestedW, int requestedH) {		
		return null;
	}
	
	static public void activateAutoFocus(Object cam) {
	}
	
	public static Object startRecording(int cameraId, int width, int height, int fps, int rotation, final long nativePtr) {
		return null;
	} 
	
	public static void stopRecording(Object cam) {
	} 
	
	public static void setPreviewDisplaySurface(Object cam, Object surf) {
	}
	
	protected static int[] selectNearestResolutionAvailableForCamera(int id, int requestedW, int requestedH) {
        return null;
	}
	
	protected static void applyCameraParameters(Camera camera, int width, int height, int requestedFps) {
	
	}
}
