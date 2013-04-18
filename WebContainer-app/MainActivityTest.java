package com.sensetecnic.container.test;

import com.sensetecnic.container.MainActivity;
import com.sensetecnic.container.R;

import android.test.ActivityInstrumentationTestCase2;
import android.webkit.WebView;

public class MainActivityTest extends
		ActivityInstrumentationTestCase2<MainActivity> {
	
	WebView webView;

	public MainActivityTest(String name) {
		super(MainActivity.class);
		System.err.println("yolo " + name);
		//super("com.sensetecnic.container", MainActivity.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		MainActivity mainActivity = getActivity();
		webView = (WebView) mainActivity.findViewById(R.id.webview);
	}
	
	public void testNavigateAway() {
		webView.loadUrl("/123/accel/start/native/accel");
		assertTrue(true);
	}

}
