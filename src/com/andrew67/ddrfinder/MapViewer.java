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

import java.util.LinkedList;
import java.util.List;

import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;


public class MapViewer extends FragmentActivity
implements ProgressBarController, MessageDisplay {
	
	public static final int BASE_ZOOM = 12;
	
	private GoogleMap mMap;
	private SupportMapFragment mMapFragment = null;
	private ProgressBar progressBar;

	private List<Marker> currentMarkers;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_viewer);
				
		currentMarkers = new LinkedList<Marker>();
		
		progressBar = (ProgressBar) findViewById(R.id.loading);
		
		mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMap = mMapFragment.getMap();
		
		LatLng home = new LatLng(18.201422,-67.145157);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(home,BASE_ZOOM));
		
		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
				updateMap();
			}
		});
	}
	
	private void updateMap()
	{
		final LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;
		new MapLoader(mMap, currentMarkers, this, this).execute(box);
	}
	
	@Override
	public void showProgressBar() {
		progressBar.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void hideProgressBar() {
		progressBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void showMessage(int resourceId) {
		Toast.makeText(this, resourceId, Toast.LENGTH_SHORT).show();
	}
}

