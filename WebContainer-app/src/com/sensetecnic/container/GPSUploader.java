package com.sensetecnic.container;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSUploader implements LocationListener  {
	
	private LocationManager locationManager;
	private String provider;
	private int period = 1000;
	private Timer timer = null;
	private String uploadURL;
	private String eventKey;
	private String sensorKey;
	private double lng, lat;

	/**
	 * Creates an instance of GPSUploader that can be used to control posting GPS data to ThingBroker
	 * @param context usually the Activity that owns this object
	 * @param uploadURL full ThingBroker URL to post data to
	 * @param parser an instance of the URLParser that corresponds to this request.
	 * 		         This is used to pull the EventKey and SensorKey
	 */
	public GPSUploader(Context context, String uploadURL, URLParser parser) {
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
	    provider = locationManager.getBestProvider(new Criteria(), false);
	    Location location = locationManager.getLastKnownLocation(provider);
	    
	    this.uploadURL = uploadURL;
		eventKey = parser.getEventKey();
		sensorKey = parser.getSensorKey();
	    
	    // Initialise the location fields
	    if (location != null) {
	      System.out.println("Provider " + provider + " has been selected.");
	      onLocationChanged(location);
	    } else {
	      System.err.println("Something really messed up happened");
	    } 
	}
	
	public void start() {
		if (timer != null) {
			timer.cancel();
		} else {
			locationManager.requestLocationUpdates(provider, period, 1, this);
		}
		timer = new Timer();
		timer.schedule(new GPSTimerTask(), period, period);
	}
	
	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		locationManager.removeUpdates(this);
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

	public void onLocationChanged(Location location) {
		synchronized (this) {
			lat = location.getLatitude();
		    lng = location.getLongitude();
		    System.out.println("lat = " + lat + " long = " + lng);			
		}
   	}

	public void onProviderDisabled(String provider) {
		System.err.println("Disabled");
	}

	public void onProviderEnabled(String provider) {
		System.err.println("Enabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		System.err.printf("Prov = %s, st = %d", provider, status);
	}
	
	private class GPSTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				JSONArray data = new JSONArray();
				data.put(lat);
				data.put(lng);
				System.out.println("GPS values are: " + data);
				
				ThingBrokerHelper.postObject(data, uploadURL, eventKey, sensorKey);
			} catch (Exception e1) {
				e1.printStackTrace(System.err);
			}
		}
	}

}
