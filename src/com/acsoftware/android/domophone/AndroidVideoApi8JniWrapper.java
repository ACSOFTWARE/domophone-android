package com.acsoftware.android.domophone;

 
public class AndroidVideoApi8JniWrapper {
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
}
