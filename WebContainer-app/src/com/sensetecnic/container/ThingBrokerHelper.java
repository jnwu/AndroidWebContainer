package com.sensetecnic.container;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class ThingBrokerHelper {

	private ThingBrokerHelper() {
		// This class is a bunch of static helpers. Instantiating shouldn't be allowed
	}
	
	public static String postJSONObject(JSONObject json, String uploadURL, String eventKey, String sensorKey) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();  		
			HttpPost httppost = new HttpPost(uploadURL);
			
			if (sensorKey != null && sensorKey.length() > 0) {	
				JSONObject temp = new JSONObject();
				temp.put(sensorKey, json);
				json = temp;
			}
			
			JSONObject info = new JSONObject();
			info.put(eventKey, json);
			
			ByteArrayEntity e = new ByteArrayEntity(info.toString().getBytes());
			e.setContentType("application/JSON");
			e.setContentEncoding("application/JSON");
			httppost.setEntity(e);
			HttpResponse response = httpclient.execute(httppost);
			return new BasicResponseHandler().handleResponse(response);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	public static String postJSONArray(JSONArray array, String uploadURL, String eventKey, String sensorKey) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();  		
			HttpPost httppost = new HttpPost(uploadURL);
			JSONObject info = new JSONObject();
			
			if (sensorKey != null && sensorKey.length() > 0) {	
				JSONObject temp = new JSONObject();
				temp.put(sensorKey, array);
				info.put(eventKey, temp);
			} else {
				info.put(eventKey, array);
			}
						
			ByteArrayEntity e = new ByteArrayEntity(info.toString().getBytes());
			e.setContentType("application/JSON");
			e.setContentEncoding("application/JSON");
			httppost.setEntity(e);
			HttpResponse response = httpclient.execute(httppost);
			return new BasicResponseHandler().handleResponse(response);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	// Uploads a file to the thing broker and returns the content id 
	public static String uploadFile(File file, String uploadURL, String eventKey, String sensorKey) {
		try {			
			MultipartEntity reqEntity = new MultipartEntity();  
			FileBody bin = new FileBody(file);
			reqEntity.addPart("file", bin);
		
			DefaultHttpClient httpclient = new DefaultHttpClient();  		
			HttpPost httppost = new HttpPost(uploadURL);
			httppost.setEntity(reqEntity);
			
			// Execute HTTP Post Request  
			HttpResponse response = httpclient.execute(httppost);
			String result = new BasicResponseHandler().handleResponse(response);
			
			JSONObject json = (JSONObject) new JSONTokener(result).nextValue();
			Object jsonData = json.get("content");
			return (String)((JSONArray)jsonData).get(0);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}
}
