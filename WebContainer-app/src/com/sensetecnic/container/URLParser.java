package com.sensetecnic.container;

import android.util.Log;

public class URLParser {
	
	String url;
	int device = DEVICE_INVALID;
	int method = METHOD_INVALID;
	String thingID = null;
	String eventKey = null;
	String sensorKey = null;
	String extra = null;
	boolean isSpecialURL = false;
	
	public static final int DEVICE_CAMERA = 1;
	public static final int DEVICE_ACCELEROMTER = 2;
	public static final int DEVICE_GALLERY = 3;
	public static final int DEVICE_MEDIA = 4;
	public static final int DEVICE_GPS = 5;
	public static final int DEVICE_TOUCH = 6;
	
	public static final int METHOD_START_ACCELEROMETER = 1;
	public static final int METHOD_STOP_ACCELEROMETER = 2;
	public static final int METHOD_INCREASE_ACCELEROMETER_INTERVAL = 3;
	public static final int METHOD_DECREASE_ACCELEROMETER_INTERVAL = 4;
	public static final int METHOD_START_CAMERA = 5;
	public static final int METHOD_GALLERY_OPEN = 6;
	public static final int METHOD_START_MEDIA = 7;
	public static final int METHOD_STOP_GPS = 8;
	public static final int METHOD_START_GPS = 9;
	public static final int METHOD_START_TOUCH = 0;
	
	public static final int METHOD_INVALID = -1;
	public static final int DEVICE_INVALID = -1;
	

	/**
	 * Creates an instance of URLParser, and parses the provided URL making it ready to be accessed through the getters
	 * @param url URL to be parsed
	 */
	public URLParser(String url) {
		this.url = url;
		parse();
	}
	
	/**
	 * @return Returns true if the provided URL is a special URL (i.e. don't navigate to it) 
	 */
	public boolean isSpecialURL() {
		// Since method, device and eventKey are required components of the URL, we consider
		// this URL not special if any of them couldn't be identified
		return method != METHOD_INVALID && device != DEVICE_INVALID && eventKey != null;
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
	
	public String getExtra() {
		return extra;
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

		switch (split.length) {
			case 6:
				extra = split[5];
			case 5:
				sensorKey = split[4];
		}
		
		if (eventKey == "native" && sensorKey == null) {
			Log.e("URLPARSER", "sensorKey must be specified for 'eventKey' = 'native'");
		}
				
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
		} else if (device.equals("gps")) {
			this.device = DEVICE_GPS;
			if (method.equals("start")) {
				this.method = METHOD_START_GPS;
			} else if (method.equals("cancel")) {
				this.method = METHOD_STOP_GPS;
			}
		} else if (device.equals("touch")) {
			this.device = DEVICE_TOUCH;
			if (method.equals("start")) {
				this.method = METHOD_START_TOUCH;
			}
		}
	}

}
