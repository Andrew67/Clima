/*
 * Copyright (c) 2013 Luis Torres
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

package com.kaiserdev.android.clima;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	//constant strings
	final static String FIRST_TIME_SETUP = "FirstTime";
	final static String KAISERDEV_WEBSITE = "http://www.kaiserdev.com";
	
	final static String CLIMA_API_BASE = "http://android.kaiserdev.com/clima/api";
	final static String STATUS_CALL = "/stats.php";	
	final static String UPLOAD_CALL = "/entry.php";
	
	final static String CURRENT_LATITUDE = "Latitude";
	final static String CURRENT_LONGITUDE = "Longitude";

	//location services
	LocationManager lm;
	private static float curr_latitude = (float) 18.201422;
	private static float curr_longitude = (float) -67.145157;
	LocationListener locList = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
		@Override
		public void onProviderEnabled(String provider) {}
		
		@Override
		public void onProviderDisabled(String provider) {
			if(provider.equals(LocationManager.NETWORK_PROVIDER))
			{lm.removeUpdates(this);}
		}
		
		@Override
		public void onLocationChanged(Location location) {
			curr_latitude = (float) location.getLatitude();
			curr_longitude = (float) location.getLongitude();
			lm.removeUpdates(this);
		}
	};
	
	//system variables
	private static boolean isLoading = true;
	private static int total_count = 0;
	private static int local_count = 0;
	private static Activity mainAct;
	
	//UI variables
	private boolean firstTime;
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	private static class StatCounter extends AsyncTask<LatLng,Void,JSONObject>{

		@Override
		protected JSONObject doInBackground(LatLng... box) {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			if(box.length > 0) //bind location data for local values
			{
				params.add(new BasicNameValuePair("LAT", "" + box[0].latitude));
				params.add(new BasicNameValuePair("LOG", "" + box[0].longitude));
				params.add(new BasicNameValuePair("RAD", "" + 1000));

			}
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(CLIMA_API_BASE+STATUS_CALL);
				if(box.length>0){post.setEntity(new UrlEncodedFormEntity(params));}
				
				HttpResponse response = client.execute(post);
				InputStream is = response.getEntity().getContent();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while(line != null){sb.append(line);line = reader.readLine();}
				
				return new JSONObject(sb.toString());
			} 
			catch(Exception e)
			{
				e.printStackTrace();
				return new JSONObject();
			}
		}
		
		@Override
		protected void onPostExecute(JSONObject result) {
			try {
				if(result.length() ==0){ throw new Exception();}
				total_count = result.getInt("Total");
				local_count = result.getString("Local").equals("--")? 0 : result.getInt("Local");
				isLoading = false;
				ViewFragment.refresh();
				
			} catch (Exception e) {
				total_count = -1;
				local_count = -1;
				isLoading = true;
				ViewFragment.refresh();
				return;
			}
		}
			
	}

	private static class Uploader extends AsyncTask<Float,Void,Boolean>{

		@Override
		protected Boolean doInBackground(Float... box) {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("LAT", "" + box[0]));
			params.add(new BasicNameValuePair("LOG", "" + box[1]));
			params.add(new BasicNameValuePair("SKY", "" + box[2]));
			params.add(new BasicNameValuePair("RAIN", ""+ box[3]));
			params.add(new BasicNameValuePair("BLITZ", ""+box[4]));
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(CLIMA_API_BASE+UPLOAD_CALL);
				post.setEntity(new UrlEncodedFormEntity(params));
				
				HttpResponse response = client.execute(post);
				InputStream is = response.getEntity().getContent();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while(line != null){sb.append(line);line = reader.readLine();}
				
				return sb.toString().equals("OK");
			} 
			catch(Exception e)
			{
				return false;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(result)
			{
				Toast.makeText(mainAct, "Sending Complete.", Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(mainAct, "Sending Failed.", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		firstTime = getPreferences(MODE_PRIVATE).getBoolean(FIRST_TIME_SETUP, true);

		//create the adapter for the views
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		new StatCounter().execute();
		
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(l != null){
			curr_latitude = (float) l.getLatitude(); 
			curr_longitude = (float)l.getLongitude();
		}
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locList);
		mainAct = this;

	}

	//returns a fragment object depending on the tab selected
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment;
			Bundle b;
			switch(position){
			case 0:
				fragment = new HomeFragment();
				b = new Bundle();
				b.putBoolean(FIRST_TIME_SETUP, firstTime);
				fragment.setArguments(b);
				return fragment;
			case 1:
				fragment = new SubmitFragment();
				return fragment;
			case 2:
				fragment = new ViewFragment();
				return fragment;
			case 3:
				fragment = new AboutFragment();
				return fragment;
			default:
				return new Fragment();
			}
		}
			
		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_home).toUpperCase(l);
			case 1:
				return getString(R.string.title_submit).toUpperCase(l);
			case 2:
				return getString(R.string.title_map).toUpperCase(l);
			case 3:
				return getString(R.string.title_about).toUpperCase(l);
			}
			return null;
		}
	}

	public static class HomeFragment extends Fragment {
		
		boolean isFirstTime;
		
		public HomeFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			isFirstTime = getArguments().getBoolean(MainActivity.FIRST_TIME_SETUP, true);
			View rootView = inflater.inflate(R.layout.fragment_home,
					container, false);
			TextView salute = (TextView) rootView.findViewById(R.id.lblWelcome);
			salute.setText(isFirstTime ? "Welcome,":"Welcome Back,");
			return rootView;
		}
	}
	
	public static class SubmitFragment extends Fragment{
		public static enum LocationStatus {DISABLED, LOCATING, READY};
		Button btnSend;
		Spinner spinSky;
		Spinner spinRain;
		Switch lightning;
		TextView lblLocStatus;
		LocationManager lmFrag;
		Location curr;
		
		LocationListener locListFrag = new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			
			@Override
			public void onProviderEnabled(String provider) {}
			
			@Override
			public void onProviderDisabled(String provider) {
				if(provider.equals(LocationManager.NETWORK_PROVIDER))
				{lmFrag.removeUpdates(this);}
			}
			
			@Override
			public void onLocationChanged(Location location) {
				curr_latitude = (float) location.getLatitude();
				curr_longitude = (float) location.getLongitude();
				setStatus(LocationStatus.READY);
				lmFrag.removeUpdates(this);
			}
		};
		
		public SubmitFragment(){}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View root = inflater.inflate(R.layout.fragment_submit, container, false);
			
			spinSky = (Spinner) root.findViewById(R.id.spinner1);
			ArrayAdapter<CharSequence> skyAdapter = ArrayAdapter.createFromResource(container.getContext(),R.array.sky_array, android.R.layout.simple_spinner_item);
			skyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinSky.setAdapter(skyAdapter);
			
			spinRain = (Spinner) root.findViewById(R.id.spinner2);
			ArrayAdapter<CharSequence> rainAdapter = ArrayAdapter.createFromResource(container.getContext(),R.array.rain_array, android.R.layout.simple_spinner_item);
			rainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinRain.setAdapter(rainAdapter);
			
			lightning = (Switch)root.findViewById(R.id.switch1);
			lightning.setTextOn(getResources().getText(R.string.lightning_switch_on));
			lightning.setTextOff(getResources().getText(R.string.lightning_switch_off));
			
			btnSend = (Button) root.findViewById(R.id.button1);
			btnSend.setEnabled(false);
			btnSend.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					sendInformation();
				}
			});
			
			lmFrag = (LocationManager) mainAct.getSystemService(LOCATION_SERVICE);
			
			lblLocStatus = (TextView) root.findViewById(R.id.lblGPSStatus);
			setStatus(LocationStatus.LOCATING);
			lblLocStatus.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					setStatus(LocationStatus.LOCATING);
					lmFrag.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListFrag);
				}
			});
			
			btnSend.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					uploadData();					
				}
			});
			lmFrag.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListFrag);
			return root;
		}
		
		private void uploadData()
		{
			Toast.makeText(mainAct, "Sending..", Toast.LENGTH_SHORT).show();
			Uploader up = new Uploader();
			up.execute(curr_latitude,curr_longitude,(float)spinSky.getSelectedItemPosition(),(float)spinRain.getSelectedItemPosition(),(float)(lightning.isChecked()? 1:0));
		}
		
		void setStatus(LocationStatus stat)
		{
			switch (stat)
			{
			case DISABLED:
				lblLocStatus.setText(R.string.gps_status_disabled);
				return;
			case LOCATING:
				lblLocStatus.setText(R.string.gps_status_searching);
				return;
			case READY:
				lblLocStatus.setText(R.string.gps_status_ready);
				btnSend.setEnabled(true);
				return;
			}
		}
		
		void sendInformation(){}
	}

	public static class ViewFragment extends Fragment{
		public ViewFragment(){}
	
		static TextView lblTotal;
		static TextView lblNear;
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
			View root = inflater.inflate(R.layout.fragment_view_tab, container, false);
			Button btnView = (Button) root.findViewById(R.id.btnViewMap);
			btnView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Bundle b = new Bundle();
					b.putFloat(CURRENT_LATITUDE, curr_latitude);
					b.putFloat(CURRENT_LONGITUDE, curr_longitude);
					
					Intent i = new Intent(getActivity(),MapViewer.class);
					i.putExtras(b);
					startActivity(i);
				}
			});
			
			lblTotal =  (TextView) root.findViewById(R.id.lblTotal);
			lblNear =  (TextView) root.findViewById(R.id.lblNear);
			refresh();
			
			ImageButton btnUpdate = (ImageButton)root.findViewById(R.id.imageButton1);
			btnUpdate.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					callStatsUpdater();
				}
			});
			return root;
		}
		
		private void callStatsUpdater(){
			StatCounter sc = new StatCounter();
			sc.execute(new LatLng(curr_latitude, curr_longitude));
		}
		
		public static void refresh()
		{
			if(isLoading){
				if(lblTotal != null){lblTotal.setText("--");}
				if(lblNear != null){lblNear.setText("--");}
			}
			else{
				if(lblTotal != null){lblTotal.setText(""+total_count);}
				if(lblNear != null){lblNear.setText(""+local_count);}
			}
		}
	}
	
	public static class AboutFragment extends Fragment{
		public AboutFragment(){}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View root = inflater.inflate(R.layout.fragment_about, container, false);
			ImageView img = (ImageView) root.findViewById(R.id.imgKDLogo);
			img.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(KAISERDEV_WEBSITE)));	
				}});
			return root;
		}
	}

}
