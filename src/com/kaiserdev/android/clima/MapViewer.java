package com.kaiserdev.android.clima;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;


public class MapViewer extends Activity {

	private class MarkerFeed{
		LatLng location;
		String timeSince;
		String description;
		float colorid;
		public MarkerFeed(LatLng location, String timeSince,
				String description, float resid) {
			this.location = location;
			this.timeSince = timeSince;
			this.description = description;
			this.colorid = resid;
		}
	}
	
	public static final int BASE_ZOOM = 8;
	
	private class MapLoader extends AsyncTask<LatLngBounds, Void, JSONArray>{

		private static final String LOADER_API_URL = "http://android.kaiserdev.com/clima/api/getdetails.php";
		@Override
		protected JSONArray doInBackground(LatLngBounds... box) {
			try {
				if(box.length == 0){throw new Exception();}
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("NE_LAT", "" + box[0].northeast.latitude));
				params.add(new BasicNameValuePair("NE_LOG", "" + box[0].northeast.longitude));
				params.add(new BasicNameValuePair("SW_LAT", "" + box[0].southwest.latitude));
				params.add(new BasicNameValuePair("SW_LOG", "" + box[0].southwest.longitude));
				
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(LOADER_API_URL);
				post.setEntity(new UrlEncodedFormEntity(params));
				
				HttpResponse response = client.execute(post);
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while(line != null){sb.append(line);line = reader.readLine();}
				return new JSONArray(sb.toString());
			} 
			catch(Exception e)
			{
				e.printStackTrace();
				return new JSONArray();
			}
		}
		
		@Override
		protected void onPostExecute(JSONArray result) {
			super.onPostExecute(result);
			ArrayList<MarkerFeed> out = new ArrayList<MapViewer.MarkerFeed>();
			try{
				JSONObject obj;
				for(int i = 0; i< result.length();i++)
				{
					obj = (JSONObject) result.get(i);
					LatLng point = new LatLng(obj.getDouble("Latitude"), obj.getDouble("Longitude")); 
					String timeSince = obj.getString("Time");
					String Description = obj.getString("Desc");
					float resId;
					switch(obj.getInt("ImageCode"))
					{
					case 0: //sunny
						resId = BitmapDescriptorFactory.HUE_GREEN;
						break;
					case 1: //cloudy
						resId = BitmapDescriptorFactory.HUE_YELLOW;
						break;
					case 2: //dark
						resId = BitmapDescriptorFactory.HUE_MAGENTA;
						break;
					case 3: //thunderstorm
						resId = BitmapDescriptorFactory.HUE_RED;
						break;
					default:
						resId = (float)-1.0;
						break;
					}
					out.add(new MarkerFeed(point,timeSince,Description,resId));
				}
				fillMap(out);
			}
			catch(Exception e)
			{
				out.clear();
				fillMap(out);
			}
		}
	}
	
	private GoogleMap mMap;
	private MapFragment mMapFragment = null;

	private ArrayList<Marker> currentMarkers;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_view);
		
		currentMarkers = new ArrayList<Marker>();
		
		mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
		mMap = mMapFragment.getMap();
		
		Bundle b = this.getIntent().getExtras();
		float lat = b.getFloat(MainActivity.CURRENT_LATITUDE);
		float log = b.getFloat(MainActivity.CURRENT_LONGITUDE);
		
		LatLng home = new LatLng(lat,log);
		CameraUpdate toHome = CameraUpdateFactory.newLatLngZoom(home,BASE_ZOOM);
		mMap.animateCamera(toHome);
		
		new AnimationWaiter().execute();
		Toast.makeText(this, "Long press anywhere to search that area.", Toast.LENGTH_LONG).show();
		mMap.setOnMapLongClickListener(new OnMapLongClickListener() {
			
			@Override
			public void onMapLongClick(LatLng arg0) {
				CameraUpdate toHome = CameraUpdateFactory.newLatLng(arg0);
				mMap.animateCamera(toHome);
				Toast.makeText(MapViewer.this, "Updating...", Toast.LENGTH_SHORT).show();
				new AnimationWaiter().execute();
			}
		});
	}
	
	private class AnimationWaiter extends AsyncTask<Long,Void,Long>{

		protected Long doInBackground(Long... params) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return (long) 0;
		}
		protected void onPostExecute(Long result) {
			super.onPostExecute(result);
			updateMap();
		}

	}
	
	private void updateMap()
	{
		LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;
		new MapLoader().execute(box);
	}
	
	private void fillMap(ArrayList<MarkerFeed> feed){
		if(feed.size() == 0)
		{
			Toast.makeText(this, "No entries available in this area.", Toast.LENGTH_SHORT).show();
			return;
		}
		while(currentMarkers.size()>0)
		{
			currentMarkers.get(0).remove();
			currentMarkers.remove(0);
		}
		for(MarkerFeed mf:feed)
		{
			if(mf.colorid != -1.0)
			{
				currentMarkers.add(
						mMap.addMarker(
								new MarkerOptions()
								.position(mf.location)
								.title(mf.timeSince)
								.snippet(mf.description)
								.icon(BitmapDescriptorFactory.defaultMarker(mf.colorid))));
			}
		}
	}
}

