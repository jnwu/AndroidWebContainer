package com.sensetecnic.container;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AccelerometerUploader implements SensorEventListener {
	
	float[] linearAccel = new float[3];
	int period = 1000;
	Timer timer = null;
	Sensor sensor;
	String uploadURL;
	String eventKey;
	String sensorKey;
	SensorManager sensorManager;
	

	public AccelerometerUploader(String uploadURL, Activity parent, URLParser parser) {
		sensorManager = (SensorManager)parent.getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.uploadURL = uploadURL;
		eventKey = parser.getEventKey();
		sensorKey = parser.getSensorKey();
	}
	
	public void start() {
		if (timer != null) {
			timer.cancel();
		} else {
			sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		timer = new Timer();
		timer.schedule(new AccelerometerTimerTask(), period, period);
	}
	
	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		sensorManager.unregisterListener(this);
	}
	
	public void setPeriod(int period) {
		this.period = period;
		if (timer != null) {
			start();
		}
	}
	
	public int getPeriod() {
		return period;
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (event.sensor == sensor) {
	            linearAccel[0] = event.values[0];
	            linearAccel[1] = event.values[1];
	            linearAccel[2] = event.values[2];
			}
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) { /* Safely ignore this */ }
	
	private class AccelerometerTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				JSONObject data = new JSONObject();
				data.put("x", linearAccel[0]);
				data.put("y", linearAccel[1]);
				data.put("z", linearAccel[2]);
				Log.d("DEBUG", "Accelerometer values are: " + data);
				
				JSONArray arr = new JSONArray();
				arr.put(linearAccel[0]);
				arr.put(linearAccel[1]);
				arr.put(linearAccel[2]);
				
				ThingBrokerHelper.postJSONObject(arr, uploadURL, eventKey, sensorKey);
			} catch (Exception e1) {
				e1.printStackTrace(System.err);
			}
		}
	}

}
