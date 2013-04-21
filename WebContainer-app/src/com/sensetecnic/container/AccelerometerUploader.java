package com.sensetecnic.container;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

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
	

	/**
	 * Creates an instance of AccelerometerUploader that can be used to control posting accel data to ThingBroker
	 * @param context usually the Activity that owns this object
	 * @param uploadURL full ThingBroker URL to post data to
	 * @param parser an instance of the URLParser that corresponds to this request.
	 * 		         This is used to pull the EventKey and SensorKey from the URL
	 */
	public AccelerometerUploader(Context context, String uploadURL, URLParser parser) {
		sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
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
	
	/**
	 * Update the period with which this posts data to ThingBroker
	 * @param period Period in milliseconds
	 */
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
				JSONArray arr = new JSONArray();
				arr.put(linearAccel[0]);
				arr.put(linearAccel[1]);
				arr.put(linearAccel[2]);
				Log.d("DEBUG", "Accelerometer values are: " + arr);
				
				ThingBrokerHelper.postObject(arr, uploadURL, eventKey, sensorKey);
			} catch (Exception e1) {
				e1.printStackTrace(System.err);
			}
		}
	}

}
