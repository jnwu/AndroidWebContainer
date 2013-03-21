/**
 * (c) 2011-2012 Sense Tecnic Systems Inc.   All rights reserved.
 */

package com.sensetecnic.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class HtmlCallbackActivity extends Activity { 

	
	public static final String URI_SEPARATOR = "?";
	public static final String URI_APPLICATION_SEPARATOR = "&";
	public static final String ABORT_CODE = "!ABORT";
	public static final String[] MODES = { "scan", "camera", "gallery", "uploadfile", "nfc", "accel", "quit", "app", "gencode", "browser" };

	public static final int idLength = 3;
	
	// request codes
	private static final int SCAN_RQ_CODE = 0;
	private static final int FILE_UPLOAD_CODE = 3;
	private static final int NFC_TRANSFER_CODE = 4;

	// request params - camera
	String name, tag, type, uploadPhotoUrl;
		
	// request params - nfc
	private String message;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.html_callback);

		System.out.println("Created");

		/* null out previous values, if any */
		name = null;
		tag = null;
		type = null;
		message = null;
		
		final String URI_PREFIX = getResources().getString(R.string.uri_prefix);
		
		Uri data = getIntent().getData(); 
		if (data != null) {
			String requestingUri = data.toString();
			if (!requestingUri.startsWith(URI_PREFIX))
				return;

			String request = requestingUri.substring(URI_PREFIX.length());
			int separatorIndex = request.indexOf(URI_SEPARATOR);

			String mode = separatorIndex == -1 ? request : request.substring(0, separatorIndex);
			System.out.println("Mode = " + mode);
			if (!isValidMode(mode))
				return;

			try {
				String strEntity = request.substring(separatorIndex+1);
				final StringEntity entity = new StringEntity(strEntity);
				entity.setContentType(URLEncodedUtils.CONTENT_TYPE);
				List<NameValuePair> parameters = URLEncodedUtils.parse(entity);
				handleRequest(mode, parameters);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Intent data is null");
		}
		System.out.println(data);
	}

	/**
	 * Given a parsed-out mode and a list of request parameters, handle the request.
	 * @param mode
	 * @param parameters
	 */
	private void handleRequest(String mode, List<NameValuePair> parameters) {

		if (mode.equals("scan")) {
			// ready to scan
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "QR_CODE_MODE");
			System.out.println("Ready to scan qr code");
			startActivityForResult(intent, SCAN_RQ_CODE);
			System.out.println("Scanning this");
		} else if (mode.equals("uploadfile")) {
			System.out.println("Starting the file browser");
			
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("name")) 
					name = pair.getValue();
				else if (pair.getName().equals("tag"))
					tag = pair.getValue();
				else if (pair.getName().equals("type"))
					type = pair.getValue();
				// ignore other parameters as irrelevant for this mode
			}
			// ready to upload
			Intent intent = new Intent("com.nexes.manager.LAUNCH");
			startActivityForResult(intent, FILE_UPLOAD_CODE);
		} else if (mode.equals("nfc")) {
			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("message"))
					message = pair.getValue();					
			}
			Intent intent = new Intent(HtmlCallbackActivity.this, NFCOperation.class);
			intent.putExtra("ndefmessage", message);
			startActivityForResult(intent, NFC_TRANSFER_CODE);
		} else if (mode.equals("accel")) {			
			Intent accel_callbackactivity = getIntent();
			accel_callbackactivity.putExtra("method", "accel");
			
			System.out.println("Set the intent call back actiity values");
			
			setResult(RESULT_OK, accel_callbackactivity);
			System.out.println("SetResult OK");
			
			finish();
		} else if (mode.equals("gencode")) {
			String code = null;
			String type = null;

			for (NameValuePair pair : parameters) {
				if (pair.getName().equals("type")) { 
					type = pair.getValue();					
				}
				else if (pair.getName().equals("code")) {
					code = pair.getValue();
				}
			}
			generateCode(type, code);
		}
	}

	private void generateCode(String type, String code) {
		if ("upc".equalsIgnoreCase(type))
			type = "UPC_A";
		else if ("qr".equalsIgnoreCase(type))
			type = "QR_CODE";

		System.out.println("generating code: type=["+type+"], code=["+code+"]");

		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");

		intent.putExtra("ENCODE_FORMAT", type);
		intent.putExtra("ENCODE_DATA", code);
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE"); 
		//intent.putExtra("ENCODE_TYPE","CODE_128");

		startActivity(intent);
		finish();
	}

	/**
	 * Called when QR code scanning or photo taking is complete.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		System.out.println("why are we not here yet");
		// QR code scan complete
		System.out.println(" Starting onActivity Result");
		System.out.println("Request Code : " + requestCode);
		System.out.println("Result Code: " + resultCode);
		
		if (requestCode == SCAN_RQ_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				
				// Scan was successful.  Replace {CODE} with scan results
				if (contents.startsWith("http://"))	{
					String finalUrl = contents;
					System.out.println("Final URL: " + finalUrl);
					
					// Save callback to be refreshed in the caller version of this app
					SharedPreferences settings = getSharedPreferences("container_prefs", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("callbackUrl", finalUrl);
	
					// Commit the edits!
					editor.commit(); 
					finish();
				} else {
					Intent qr_callbackactivity = getIntent();
					qr_callbackactivity.putExtra("method", "qr");
					qr_callbackactivity.putExtra("qr_result", contents);
					setResult(RESULT_OK, qr_callbackactivity);
					// start async task to post QR code activity
					//new QRCodeActivityTask().execute(contents, "I scanned a QR code!");
					finish();
				}
			} else {
				// Canceled, use abort code
				SharedPreferences settings = getSharedPreferences("container_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("callbackUrl", ABORT_CODE);

				// Commit the edits!
				editor.commit();
				finish();
			}
		} else if (requestCode == FILE_UPLOAD_CODE)	{
			if(resultCode == RESULT_OK) {
				String resultstring = intent.getStringExtra("filepath");
				System.out.println("Result string: " + resultstring);
				//photo = new File (resultstring);
				//pd = ProgressDialog.show(this, "", 	"Uploading File ...", true);
				//new PostPhotoTask().execute();
			}
		} else if (requestCode == NFC_TRANSFER_CODE) {
			if(resultCode == RESULT_OK) {
				String resultstring = intent.getStringExtra("nfc_value");
				System.out.println("NFC result String: " + resultstring);
				Intent nfc_callbackactivity = getIntent();
				nfc_callbackactivity.putExtra("uploadResult", resultstring);
				nfc_callbackactivity.putExtra("method", "nfc");
				setResult(RESULT_OK, nfc_callbackactivity);
				finish();
			}
		}
		
		System.out.println(" Finishing onActivity Result");
	}

	/**
	 * Helper to determine if a mode is supported by this activity.
	 * @param mode
	 * @return
	 */
	private boolean isValidMode(String mode) {
		for (int i = 0; i < MODES.length; i++)
			if (MODES[i].equals(mode))
				return true;

		return false;
	}
}
