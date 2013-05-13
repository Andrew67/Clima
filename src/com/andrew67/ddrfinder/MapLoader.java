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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapLoader extends AsyncTask<LatLngBounds, Void, List<ArcadeLocation>>{

		private static final String LOADER_API_URL = "http://www.ddrfinder.tk/locate.php";
		private GoogleMap map;
		private List<Marker> markers;
		private ProgressBarController pbc;
		private MessageDisplay display;
		
		public MapLoader(GoogleMap map, List<Marker> markers,
				ProgressBarController pbc, MessageDisplay display) {
			super();
			this.map = map;
			this.markers = markers;
			this.pbc = pbc;
			this.display = display;
			
			// Show indeterminate progress bar
			// Assumes this class is constructed followed by a call to execute()
			// where the bar is hidden on data load completion
			pbc.showProgressBar();
		}
		
		@Override
		protected List<ArcadeLocation> doInBackground(LatLngBounds... box) {
			// Fetch machine data in JSON format
			JSONArray result = new JSONArray();
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
				Log.d("api", "" + response.getStatusLine().getStatusCode());
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while(line != null){sb.append(line);line = reader.readLine();}
				Log.d("api", sb.toString());
				result = new JSONArray(sb.toString());
			} 
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			// Return list
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
			}
			catch(Exception e)
			{
				out.clear();
			}
			return out;
		}
		
		@Override
		protected void onPostExecute(List<ArcadeLocation> result) {
			super.onPostExecute(result);
			pbc.hideProgressBar();
			fillMap(result);
		}
		
		private void fillMap(List<ArcadeLocation> feed){
			if(feed.size() == 0)
			{
				return;
			}
			while(markers.size()>0)
			{
				markers.get(0).remove();
				markers.remove(0);
			}
			for(ArcadeLocation mf:feed)
			{
				markers.add(
						map.addMarker(
								new MarkerOptions()
								.position(mf.getLocation())
								.title(mf.getName())
								.snippet(mf.getCity())));
			}
		}
	}