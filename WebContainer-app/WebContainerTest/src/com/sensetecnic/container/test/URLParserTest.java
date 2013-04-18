package com.sensetecnic.container.test;

import com.sensetecnic.container.URLParser;

import junit.framework.TestCase;

public class URLParserTest extends TestCase {

	public URLParserTest() {
		super("com.sensetecnic.container");
	}
	
	private static final String[] testDevices = {"camera", "gallery", "accelerometer", "gps", "media"};
	private static final String[] testMethods = {"start", "open", "stop", "start", "start"};
	
	private static final int[] expectedDevices = {
		URLParser.DEVICE_CAMERA, URLParser.DEVICE_GALLERY, 
		URLParser.DEVICE_ACCELEROMTER, URLParser.DEVICE_GPS, URLParser.DEVICE_MEDIA};
	
	private static final int[] expectedMethods = {
		URLParser.METHOD_START_CAMERA, URLParser.METHOD_GALLERY_OPEN, URLParser.METHOD_STOP_ACCELEROMETER,
		URLParser.METHOD_START_GPS, URLParser.METHOD_START_MEDIA};
	
	public void testFullURL() {
		for (int i = 0; i < testDevices.length; i++) {
			String url = "http://google.com/thingId/" + testDevices[i] + "/" + testMethods[i] + "/native/key";
			System.err.println("url: " + url);
			URLParser parser = new URLParser(url);
			assertTrue(parser.isSpecialURL());
			assertEquals("thingId", parser.getThingID());
			assertEquals("native", parser.getEventKey());
			assertEquals("key", parser.getSensorKey());
			assertEquals(expectedDevices[i], parser.getDevice());
			assertEquals(expectedMethods[i], parser.getMethod());
		}
	}
	
	public void testBrokenURL() {
		String url = "http://google.com/123/camera/";
		URLParser parser = new URLParser(url);
		assertFalse(parser.isSpecialURL());
		assertEquals(URLParser.METHOD_INVALID, parser.getMethod());
		
		url = "http://google.com/123/";
		parser = new URLParser(url);
		assertFalse(parser.isSpecialURL());
		assertEquals(URLParser.METHOD_INVALID, parser.getMethod());
		assertEquals(URLParser.DEVICE_INVALID, parser.getDevice());
		
		url = "http://google.com/123/camera/randomMethod/native";
		parser = new URLParser(url);
		assertFalse(parser.isSpecialURL());
		assertEquals(URLParser.METHOD_INVALID, parser.getMethod());
		assertEquals(URLParser.DEVICE_CAMERA, parser.getDevice());
		
		url = "http://google.com/123/randomDevice/start/native/sensor";
		parser = new URLParser(url);
		assertFalse(parser.isSpecialURL());
		assertEquals(URLParser.DEVICE_INVALID, parser.getDevice());
	}

}
