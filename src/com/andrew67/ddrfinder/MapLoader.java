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

import com.andrew67.ddrfinder.data.ApiResult;
import com.andrew67.ddrfinder.data.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapLoader extends AsyncTask<LatLngBounds, Void, ApiResult>{

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
		protected ApiResult doInBackground(LatLngBounds... box) {
			// Fetch machine data in JSON format
			JSONArray jArray = new JSONArray();
			try {
				if (box.length == 0) throw new Exception();
				final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("source", "android"));
				params.add(new BasicNameValuePair("latupper", "" + box[0].northeast.latitude));
				params.add(new BasicNameValuePair("longupper", "" + box[0].northeast.longitude));
				params.add(new BasicNameValuePair("latlower", "" + box[0].southwest.latitude));
				params.add(new BasicNameValuePair("longlower", "" + box[0].southwest.longitude));
				
				final HttpClient client = new DefaultHttpClient();
				final HttpGet get = new HttpGet(LOADER_API_URL + "?" + URLEncodedUtils.format(params, "utf-8"));
				
				final HttpResponse response = client.execute(get);
				final int statusCode = response.getStatusLine().getStatusCode();
				Log.d("api", "" + statusCode);
				
				// Data loaded OK
				if (statusCode == 200) {
					final InputStream is = response.getEntity().getContent();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					final StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					Log.d("api", sb.toString());
					jArray = new JSONArray(sb.toString());
				}
				// Code used for invalid parameters; in this case exceeding
				// the limits of the boundary box
				else if (statusCode == 400) {
					return new ApiResult(ApiResult.ERROR_ZOOM);
				}
				// Unexpected error code
				else {
					return new ApiResult(ApiResult.ERROR_API);
				}
			} 
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			// Return list
			ArrayList<ArcadeLocation> out = new ArrayList<ArcadeLocation>();
			try{
				for (int i = 0; i < jArray.length(); ++i)
				{
					final JSONObject obj = (JSONObject) jArray.get(i);
					out.add(new ArcadeLocation(
							obj.getInt("id"),
							obj.getString("name"),
							obj.getString("city"),
							new LatLng(obj.getDouble("latitude"),
									obj.getDouble("longitude"))
							));
				}
			}
			catch(Exception e)
			{
				out.clear();
			}
			return new ApiResult(out);
		}
		
		@Override
		protected void onPostExecute(ApiResult result) {
			super.onPostExecute(result);
			pbc.hideProgressBar();
			
			switch(result.getErrorCode()) {
			case ApiResult.ERROR_NONE:
				fillMap(result.getLocations());
				break;
			case ApiResult.ERROR_ZOOM:
				display.showMessage(R.string.error_zoom);
				break;
			default:
				display.showMessage(R.string.error_api);
			}
		}
		
		private void fillMap(List<ArcadeLocation> feed){
			if (feed.size() == 0)
			{
				return;
			}
			for (Marker marker : markers) {
				marker.remove();
			}
			markers.clear();
			for (ArcadeLocation loc : feed)
			{
				markers.add(
						map.addMarker(
								new MarkerOptions()
								.position(loc.getLocation())
								.title(loc.getName())
								.snippet(loc.getCity())));
			}
		}
	}