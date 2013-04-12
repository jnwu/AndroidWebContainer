package com.sensetecnic.container;

import android.util.Log;

public class URLParser {
	
	String url;
	int device = -1;
	int method = -1;
	String thingID = null;
	String eventKey = null;
	String sensorKey = null;
	boolean isSpecialURL = false;
	
	public static final int DEVICE_CAMERA = 1;
	public static final int DEVICE_ACCELEROMTER = 2;
	public static final int DEVICE_GALLERY = 3;
	public static final int DEVICE_MEDIA = 4;
	
	public static final int METHOD_START_ACCELEROMETER = 1;
	public static final int METHOD_STOP_ACCELEROMETER = 2;
	public static final int METHOD_INCREASE_ACCELEROMETER_INTERVAL = 3;
	public static final int METHOD_DECREASE_ACCELEROMETER_INTERVAL = 4;
	public static final int METHOD_START_CAMERA = 5;
	public static final int METHOD_GALLERY_OPEN = 6;
	public static final int METHOD_START_MEDIA = 7;
	

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
	
	public String getEventKey() {
		return eventKey;
	}
	
	public String getSensorKey() {
		return sensorKey;
	}
	
	private void parse() {
		url = url.substring(url.indexOf('/', 7) + 1);
		String[] split = url.split("/");
		// Minimum length is 4 for (thingId, sensorId, method, eventKey)
		if (split.length < 4) {
			Log.e("URLParser", "Coudln't parse: '" + url + "'");
			return;
		}
		thingID = split[0];
		String device = split[1];
		String method = split[2];
		eventKey = split[3];
		if (split.length > 4) 
			sensorKey = split[4];
		else if (eventKey == "native") {
			Log.e("URLPARSER", "sensorKey must be specified for 'eventKey' = 'native'");
		}
		System.out.printf(
			"device=%s method=%s eventKey=%s sensorKey=%s\n", device, method, eventKey, sensorKey);
		
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
		} else if (device.equals("media")) {
			this.device = DEVICE_MEDIA;
			if (method.equals("start")) {
				this.method = METHOD_START_MEDIA;
			}
		}
	}

}
