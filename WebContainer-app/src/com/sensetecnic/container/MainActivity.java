/**
 * (c) 2011-2012 Sense Tecnic Systems Inc.   All rights reserved.
 */

package com.sensetecnic.container;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONTokener;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class MainActivity extends SlidingFragmentActivity {

	private WebView webView;
	private int accelerometerPeriod = 1000;
	private boolean accelerometer_enabled = false;
	private JSONArray appList;
	private String displayID = "";
	private String thingbrokerServer;
	private String thingbrokerPort;
	
	protected static final int CONFIGURE_THINGBROKER_SERVER = 1;
	protected static final int CONFIGURE_THINGBROKER_PORT = 2;
	protected static final int CHANGE_URL = 3;
	
	private ProgressBar pbLoading;
	
	// For uploading accelerometer and GPS data periodically
	AccelerometerUploader accelerometerUploader;
	GPSUploader gpsUploader;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.html_challenge);
		setBehindContentView(R.layout.menu_frame);
		
		initAppList();
		
		SlidingMenu menu = getSlidingMenu();
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
		
        thingbrokerServer = getString(R.string.thing_broker_server);
        thingbrokerPort = getString(R.string.thing_broker_port);
		pbLoading = (ProgressBar)findViewById(R.id.pbLoading);
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.setWebChromeClient(new ContainerWebChromeClient());
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return onURLChange(url);
			}
		});

		// load default url on start
		webView.loadUrl(getString(R.string.default_url));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.html_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
			webView.reload();
			return true;
		case R.id.gotourl:
			showPromptDialog(CHANGE_URL);
			return true;
		case R.id.thingbroker_server:
			showPromptDialog(CONFIGURE_THINGBROKER_SERVER);
			return true;
		case R.id.thingbroker_port:
			showPromptDialog(CONFIGURE_THINGBROKER_PORT);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	protected void onSaveinstanceState(Bundle outState) {
		webView.saveState(outState);
	}
	
	/**
	 * Helper function that is used for showing  a dialog that allows the user
	 * to configure a part of the application
	 * @param action One of the private constants declared in this class
	 * 		  (e.g. CONFIGURE_THINGBROKER_SERVER) 
	 */
	private void showPromptDialog(final int action) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		final LinearLayout layout = new LinearLayout(this);
		final TextView instructions = new TextView(this);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		input.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f));
		switch(action) {
			case CONFIGURE_THINGBROKER_SERVER:
				input.setText(thingbrokerServer);
				instructions.setText("Enter Server URL (without protocol):");
				break;
			case CONFIGURE_THINGBROKER_PORT:
				input.setText(thingbrokerPort);
				instructions.setText("Enter Port Number:");
				break;
			case CHANGE_URL:
				input.setText(webView.getUrl());
				instructions.setText("Enter URL:");
				break;
		}
		instructions.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(instructions);
		layout.addView(input);
		alert.setView(layout);
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				switch(action) {
					case CONFIGURE_THINGBROKER_SERVER:
						thingbrokerServer = value;
						break;
					case CONFIGURE_THINGBROKER_PORT:
						thingbrokerPort = value;
						break;
					case CHANGE_URL:
						webView.loadUrl(value);
						break;
				}
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		alert.show();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (accelerometer_enabled) {
			accelerometerUploader.start();
		}
	}

	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack();
		} else {
			accelerometerUploader.stop();
			finish();
		}
	}

    @Override
    protected void onStop() {
    	if (accelerometerUploader != null)
    		accelerometerUploader.stop();
        super.onStop();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanResult != null) {
				String content = scanResult.getContents();
				displayID = content.substring(content.lastIndexOf('/') + 1, content.length());
				System.out.println("DisplayID = " + displayID);
			}
		}
	}

	 // Custom WebChromeClient that allows us to show the progress as the WebView loads the web page
	class ContainerWebChromeClient extends WebChromeClient {
		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
			callback.invoke(origin, true, false);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			new AlertDialog.Builder(view.getContext()).setMessage(message).setCancelable(true).show();
			result.confirm();
			return true;
		}

		@Override
		public void onProgressChanged(WebView view, int progress) {
			if (progress < 100 && pbLoading.getVisibility() == ProgressBar.GONE) {
				pbLoading.setVisibility(ProgressBar.VISIBLE);				
			}
			pbLoading.setProgress(progress);
			if (progress == 100) {
				pbLoading.setVisibility(ProgressBar.GONE);
			}
		}
	}
	
	// This method issues a GET request to the Container API URL and receives a list of
	// applications that are to be displayed
	private void initAppList() {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet(getString(R.string.api_url));
			HttpResponse response = httpclient.execute(getRequest);
			System.out.println(response.getStatusLine());
			if (response.getEntity() != null) {
				String result = new BasicResponseHandler().handleResponse(response);
				appList = (JSONArray) new JSONTokener(result).nextValue();
				
				String[] items = new String[appList.length() + 1];
				for (int i = 0; i < appList.length(); i++) {
					items[i] = appList.getJSONObject(i).getString("name");
				}
				
				// Hardcode last item for scanning the QR code of the display
				items[items.length - 1] = "** Scan Display QR **";
				
				getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.menu_frame, new MenuFragment(items))
					.commit();
			} else {
				Log.e("ERROR", "Unable to fetch the list of apps");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}		
	}
	
	/**
	 * This method handles the processing of URLs, and taking the right action accordingly
	 * (i.e. starting new Activity, stopping upload, etc)
	 * @param url The URL user is attempting to navigate
	 * @return true if this is a special URL that requires our handling, false otherwise
	 */
	private boolean onURLChange(String url) {
		URLParser parser = new URLParser(url);
		if (parser.isSpecialURL()) {
			String uploadURL = getUploadURL(parser.getThingID(), displayID);
			if (parser.getDevice() == URLParser.DEVICE_ACCELEROMTER) {
				if (accelerometerUploader != null) {
					accelerometerUploader.stop();
				}
				accelerometerUploader = new AccelerometerUploader(this, uploadURL, parser);
			}
			Intent intent;
			switch(parser.getMethod()) {
				case URLParser.METHOD_START_ACCELEROMETER:
					new GPSUploader(this, uploadURL, parser);
					accelerometerUploader.start();
					accelerometer_enabled = true;
					return true;
				case URLParser.METHOD_STOP_ACCELEROMETER:
					accelerometerUploader.stop();
					accelerometer_enabled = false;
					return true;
				case URLParser.METHOD_INCREASE_ACCELEROMETER_INTERVAL:
				case URLParser.METHOD_DECREASE_ACCELEROMETER_INTERVAL:
					accelerometerPeriod *=
						(parser.getMethod() == URLParser.METHOD_DECREASE_ACCELEROMETER_INTERVAL) ? 0.5 : 2;
					accelerometerUploader.setPeriod(accelerometerPeriod);
					if (accelerometer_enabled) {
						accelerometerUploader.start();
					}
					System.out.println("accelPeriod = " + accelerometerPeriod);
					return true;
				case URLParser.METHOD_START_CAMERA:
				case URLParser.METHOD_GALLERY_OPEN:
					intent = new Intent(MainActivity.this, PhotoUploader.class);
					String source = (parser.getMethod() == URLParser.METHOD_GALLERY_OPEN) ?
									"gallery" : "camera";
					intent.putExtra("source", source);
					intent.putExtra("uploadURL", uploadURL);
					intent.putExtra("eventKey", parser.getEventKey());
					intent.putExtra("sensorKey", parser.getSensorKey());
					startActivityForResult(intent, 1);
					return true;
				case URLParser.METHOD_START_MEDIA:
					intent = new Intent(MainActivity.this, MediaUploaderActivity.class);
					intent.putExtra("uploadURL", uploadURL);
					intent.putExtra("eventKey", parser.getEventKey());
					intent.putExtra("sensorKey", parser.getSensorKey());
					startActivityForResult(intent, 1);
					return true;
				case URLParser.METHOD_START_GPS:
					if (gpsUploader == null) {
						gpsUploader = new GPSUploader(this, uploadURL, parser);
					}
					gpsUploader.start();
					return true;
				case URLParser.METHOD_STOP_GPS:
					if (gpsUploader != null) {
						gpsUploader.stop();
					}
					return true;
				case URLParser.METHOD_START_TOUCH:
					ThingBrokerHelper.postObject(
							parser.getExtra(),
							uploadURL,
							parser.getEventKey(),
							parser.getSensorKey());
					return true;
			}
		}
		
		System.out.println("Ordinary URL: " + url);
		return false;
	}
	
	public String getUploadURL(String thingID, String displayID) {
		// Follow this format: http://kimberly.magic.ubc.ca:8080/thingbroker/things/1231/events
		String baseURL = "http://" + thingbrokerServer + ":" + thingbrokerPort + "/thingbroker";
		return baseURL + "/things/" + thingID + displayID + "/events";
	}

	// Called when a user makes a selection from the SlidingMenu
	public void onMenuItemSelected(int index) {
		toggle();
		// If this is the last time in the list, then it's for scanning the QR code of a display
		if (index == appList.length()) {
			IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
			integrator.initiateScan();
		} else {
			try {
				String url = appList.getJSONObject(index).getString("mobile_url");
				if (url == null || url.equals("")) {
					url = appList.getJSONObject(index).getString("url");
				}
				webView.loadUrl(url);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}