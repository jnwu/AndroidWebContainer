package com.sensetecnic.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class PhotoUploader extends Activity {
	
	long captureTime;
	File photo;
	ProgressDialog progressDialog;
	String uploadURL;
	String eventKey;
	String sensorKey;
	
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int CAPTURE_IMAGE = 1;
	private static final int GALLERY_IMAGE = 2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.html_callback);
		
		Intent intent = getIntent();
		String source = intent.getStringExtra("source");
		uploadURL = intent.getStringExtra("uploadURL") + "?keep-stored=true";
		eventKey = intent.getStringExtra("eventKey");
		sensorKey = intent.getStringExtra("sensorKey"); 
		if (source.equals("camera")) {
			captureTime = System.currentTimeMillis();
			Intent newIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
			System.out.println("Starting the Photo Taking");
			
			photo = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			Uri cameraFileUri = Uri.fromFile(photo); // create a file to save the image
			newIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri); // set the image file name
			startActivityForResult(newIntent, CAPTURE_IMAGE);
		} else if (source.equals("gallery")) {
			Intent newIntent = new Intent(Intent.ACTION_GET_CONTENT);
			newIntent.setType("image/*");
			startActivityForResult(Intent.createChooser(newIntent, "Select Photo"), GALLERY_IMAGE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			try {
				progressDialog = ProgressDialog.show(this, "", 	"Compressing photo...", true);
				Uri uri = null;
				if (requestCode == CAPTURE_IMAGE) {
					uri = Uri.fromFile(photo);
				} else if (requestCode == GALLERY_IMAGE) {
					uri = data.getData();
					String[] filePathColumn = {MediaStore.Images.Media.DATA};
					Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
					int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
					cursor.moveToFirst();
					String path = cursor.getString(columnIndex);
					cursor.close();
					uri = Uri.fromFile(new File(path));
				}
				new CompressAndUploadPhotoTask().execute(uri); 		            
			} catch (Exception e) {
				e.printStackTrace(System.err);
				finish();
			}
		} else {
			finish();
		}		
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
		File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/meeImages");

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("MEE", "Failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
						"IMG_"+ timeStamp + ".jpg");
		} else {
			return null;
		}
		return mediaFile;
	}
	
	class CompressAndUploadPhotoTask extends AsyncTask<Uri, Void, Boolean> {
		protected Boolean doInBackground(Uri... params) {
			try {
				InputStream imageStream = getContentResolver().openInputStream(params[0]);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 4;
				Bitmap image = BitmapFactory.decodeStream(imageStream, null, options);
				if (photo == null) {
					//photo = getOutputMediaFile(MEDIA_TYPE_IMAGE);
					photo = new File(params[0].getPath());
				}

				int rotation = -1, rotateDegrees = 0;
				long fileSize = photo.length();
				Cursor mediaCursor = PhotoUploader.this.getContentResolver().query(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						new String[] {MediaStore.Images.ImageColumns.ORIENTATION, MediaStore.MediaColumns.SIZE },
						MediaStore.MediaColumns.DATE_ADDED + ">=?",
						new String[]{String.valueOf(captureTime/1000 - 1)},
						MediaStore.MediaColumns.DATE_ADDED + " desc");
				if (mediaCursor != null && captureTime != 0 && mediaCursor.getCount() !=0 ) {
					while(mediaCursor.moveToNext()) {
						long size = mediaCursor.getLong(1);
						//Extra check to make sure that we are getting the orientation from the proper file
						if(size == fileSize) {
							rotation = mediaCursor.getInt(0);
							break;
						}
					}
				}
				if (rotation == -1) {
					ExifInterface exif = new ExifInterface(photo.getAbsolutePath());
					rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
					rotateDegrees = 0;
				}
				switch(rotation) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						rotateDegrees-=90;
					case ExifInterface.ORIENTATION_ROTATE_180:
						rotateDegrees-=90;
					case ExifInterface.ORIENTATION_ROTATE_270:
						rotateDegrees-=90;
				}
				Matrix matrix = new Matrix();
				matrix.postRotate(rotateDegrees);
				image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

				FileOutputStream out = new FileOutputStream(photo);
				
				image.compress(CompressFormat.JPEG,  90,  out);

				System.out.println("Compress and Upload Task Completed");
				return true;
			} catch (Exception e) {
				return false;
			} 
		}
	
		protected void onPostExecute(Boolean result) {
			System.out.println("On Post Execute");
			if (result == false)
				finish();

			if (progressDialog != null)
				progressDialog.dismiss();
			progressDialog = ProgressDialog.show(PhotoUploader.this, "", "Uploading photo...", true);
			new PostPhotoTask().execute();
		}
	}
	
	// uploads photo we just took to the server, and then finishes up	
	class PostPhotoTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... params) {
			try {
				String id = ThingBrokerHelper.uploadFile(photo, uploadURL, eventKey, sensorKey);
				JSONArray array = new JSONArray();

				String src = 
					"http://" + getString(R.string.thing_broker_server) + ":" 
					+ getString(R.string.thing_broker_port) + "/thingbroker/content/" + id;
				array.put(src);

				return ThingBrokerHelper.postJSONArray(array, uploadURL, eventKey, sensorKey);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		}
		
		protected void onPostExecute(String result) {
			if (progressDialog != null)
				progressDialog.dismiss();

			if (result != null) {
				System.out.println("Uploaded to " + getString(R.string.upload_url) + " with response " + result);
				
				//Passes the result of the upload back out of the callbackactivity
				Intent callbackactivity = getIntent();
				callbackactivity.putExtra("uploadResult", result);
				callbackactivity.putExtra("method", "upload");
				setResult(RESULT_OK, callbackactivity);
			}
			
			finish();
		}
	}

}
