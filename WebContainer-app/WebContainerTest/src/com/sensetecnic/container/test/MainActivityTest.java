package com.sensetecnic.container.test;

import com.sensetecnic.container.MainActivity;
import com.sensetecnic.container.MediaUploaderActivity;
import com.sensetecnic.container.PhotoUploader;
import com.sensetecnic.container.R;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.MediaStore;
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.WebView;

public class MainActivityTest extends
		ActivityInstrumentationTestCase2<MainActivity> {
	
	WebView webView;
	Instrumentation inst;
	MainActivity activity;

	public MainActivityTest() {
		super(MainActivity.class);
		//super("com.sensetecnic.container", MainActivity.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
		webView = (WebView) activity.findViewById(R.id.webview);
		inst = getInstrumentation();
	}
	
	protected void tearDown() throws Exception {
		activity.finish();
		super.tearDown();
	}

	// TODO: figure out a way to split these tests
	public void testURLInteception() {
		// URL for starting camera
		IntentFilter filter = new IntentFilter();
		filter.addAction(MediaStore.ACTION_IMAGE_CAPTURE);
		ActivityMonitor monitor = inst.addMonitor(filter, null, true);
		webView.loadUrl("http://google.com/123/camera/start/native/photo");
		monitor.waitForActivityWithTimeout(5000);
		assertEquals(1, monitor.getHits());
		
		// URL for opening gallery
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_GET_CONTENT);
		inst.removeMonitor(monitor);
		monitor = inst.addMonitor(filter, null, true);
		webView.loadUrl("http://google.com/123/gallery/open/native/photo");
		monitor.waitForActivityWithTimeout(5000);
		assertEquals(1, monitor.getHits());
		
		// URL for starting file upload
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_VIEW);
		inst.removeMonitor(monitor);
		monitor = inst.addMonitor(filter, null, false);
		webView.loadUrl("http://google.com/123/media/start/native/file");
		Activity r = monitor.waitForActivityWithTimeout(5000);
		assertNotNull(r);
		r.finish();
		assertTrue(r instanceof MediaUploaderActivity);
		assertEquals(1, monitor.getHits());
	}
	
	/*
	public void testStartGallery() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_GET_CONTENT);
		ActivityMonitor monitor = inst.addMonitor(filter, null, false);
		webView.loadUrl("http://google.com/123/gallery/open/native/photo");
		monitor.waitForActivityWithTimeout(5000);
		assertEquals(1, monitor.getHits());
	}
	*/
}
