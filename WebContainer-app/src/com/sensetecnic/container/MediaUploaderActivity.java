package com.sensetecnic.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sensetecnic.container.PhotoUploader.CompressAndUploadPhotoTask;
import com.sensetecnic.container.PhotoUploader.PostPhotoTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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

public class MediaUploaderActivity extends Activity {

	File file;
	ProgressDialog progressDialog;
	String uploadURL;
	String eventKey;
	String sensorKey;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.html_callback);
		
		Intent intent = getIntent();
		uploadURL = intent.getStringExtra("uploadURL") + "?keep-stored=true";
		eventKey = intent.getStringExtra("eventKey");
		sensorKey = intent.getStringExtra("sensorKey");
		
		Intent newIntent = new Intent("com.nexes.manager.LAUNCH");
		startActivityForResult(newIntent, 0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Intent callbackactivity = getIntent();
		if (resultCode == RESULT_OK) {
			try {
				progressDialog = ProgressDialog.show(this, "", 	"Uploading file...", true);
				file = new File(data.getStringExtra("filepath"));
				new FileUploadTask().execute();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				finish();
			}
		} else {
			finish();
		}
	}
	
	// uploads file we just selected to the server, and then finishes up	
	class FileUploadTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... params) {
			try {
				String id = ThingBrokerHelper.uploadFile(file, uploadURL, eventKey, sensorKey);
				JSONArray array = new JSONArray();

				String src = 
					"http://" + getString(R.string.thing_broker_server) + ":" 
					+ getString(R.string.thing_broker_port) + "/thingbroker/content/" + id;
				array.put(src);
				array.put(file.getName());

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
				//System.out.println("Uploaded to " + getString(R.string.upload_url) + " with response " + result);
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
