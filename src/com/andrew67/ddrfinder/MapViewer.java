/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 * 
 * Copyright (c) 2013 Andrés Cordero 
 * Web: https://github.com/Andrew67/DdrFinder
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.andrew67.ddrfinder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;


public class MapViewer extends FragmentActivity {
	
	public static final int BASE_ZOOM = 12;
	
	private class MapLoader extends AsyncTask<LatLngBounds, Void, JSONArray>{

		private static final String LOADER_API_URL = "http://www.ddrfinder.tk/locate.php";
		@Override
		protected JSONArray doInBackground(LatLngBounds... box) {
			try {
				if(box.length == 0){throw new Exception();}
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("source", "android"));
				params.add(new BasicNameValuePair("latupper", "" + box[0].northeast.latitude));
				params.add(new BasicNameValuePair("longupper", "" + box[0].northeast.longitude));
				params.add(new BasicNameValuePair("latlower", "" + box[0].southwest.latitude));
				params.add(new BasicNameValuePair("longlower", "" + box[0].southwest.longitude));
				
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(LOADER_API_URL + "?" + URLEncodedUtils.format(params, "utf-8"));
				
				HttpResponse response = client.execute(get);
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while(line != null){sb.append(line);line = reader.readLine();}
				Log.d("api", sb.toString());
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
			ArrayList<ArcadeLocation> out = new ArrayList<ArcadeLocation>();
			try{
				JSONObject obj;
				for(int i = 0; i< result.length();i++)
				{
					obj = (JSONObject) result.get(i);
					int id = obj.getInt("id");
					String name = obj.getString("name");
					String city = obj.getString("city");
					LatLng location = new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude"));
					out.add(new ArcadeLocation(id, name, city, location));
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
	private SupportMapFragment mMapFragment = null;

	private ArrayList<Marker> currentMarkers;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_view);
		
		currentMarkers = new ArrayList<Marker>();
		
		mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMap = mMapFragment.getMap();
		
		LatLng home = new LatLng(18.201422,-67.145157);
		CameraUpdate toHome = CameraUpdateFactory.newLatLngZoom(home,BASE_ZOOM);
		mMap.animateCamera(toHome);
		
		new AnimationWaiter().execute();
		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
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
	
	private void fillMap(ArrayList<ArcadeLocation> feed){
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
		for(ArcadeLocation mf:feed)
		{
			currentMarkers.add(
					mMap.addMarker(
							new MarkerOptions()
							.position(mf.getLocation())
							.title(mf.getName())
							.snippet(mf.getCity())));
		}
	}
}

