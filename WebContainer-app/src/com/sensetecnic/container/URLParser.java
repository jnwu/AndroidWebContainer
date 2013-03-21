package com.sensetecnic.container;

import android.util.Log;

public class URLParser {
	
	String url;
	int device = -1;
	int method = -1;
	String thingID = null;
	boolean isSpecialURL = false;
	
	public static final int DEVICE_CAMERA = 1;
	public static final int DEVICE_ACCELEROMTER = 2;
	public static final int DEVICE_GALLERY = 3;
	
	public static final int METHOD_START_ACCELEROMETER = 1;
	public static final int METHOD_STOP_ACCELEROMETER = 2;
	public static final int METHOD_INCREASE_ACCELEROMETER_INTERVAL = 3;
	public static final int METHOD_DECREASE_ACCELEROMETER_INTERVAL = 4;
	public static final int METHOD_START_CAMERA = 5;
	public static final int METHOD_GALLERY_OPEN = 6;
	

	public URLParser(String url) {
		this.url = url;
		parse();
	}
	
	public boolean isSpecialURL() {
		return method != -1 && device != -1;
	}
	
	public int getDevice() {
		return device;
	}
	
	public int getMethod() {
		return method;
	}
	
	public String getThingID() {
		return thingID;
	}
	
	private void parse() {
		url = url.substring(url.indexOf('/', 7) + 1);
		String[] split = url.split("/");
		if (split.length < 3) {
			Log.e("URLParser", "Coudln't parse: '" + url + "'");
			return;
		}
		thingID = split[0];
		String device = split[1];
		String method = split[2];
		System.out.println("url = " + url  + " device = " + device + " method = " + method);
		
		if (device.equals("camera")) {
			this.device = DEVICE_CAMERA;
			if (method.equals("start")) {
				this.method = METHOD_START_CAMERA;
			}
		} else if (device.equals("accelerometer")) {
			this.device = DEVICE_ACCELEROMTER;
			if (method.equals("start")) {
				this.method = METHOD_START_ACCELEROMETER;
			} else if (method.equals("stop")) {
				this.method = METHOD_STOP_ACCELEROMETER;
			} else if (method.equals("increaseInterval")) {
				this.method = METHOD_INCREASE_ACCELEROMETER_INTERVAL;
			} else if (method.equals("decreaseInterval")) {
				this.method = METHOD_DECREASE_ACCELEROMETER_INTERVAL;
			}
		} else if (device.equals("gallery")) {
			this.device = DEVICE_GALLERY;
			if (method.equals("open")) {
				this.method = METHOD_GALLERY_OPEN;
			}
		}
	}

}
