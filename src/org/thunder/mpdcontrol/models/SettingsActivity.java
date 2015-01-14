package org.thunder.mpdcontrol.models;

import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.mpd.MPDStatus;
import org.thunder.mpdcontrol.mpd.event.StatusChangeListener;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public abstract class SettingsActivity extends PreferenceActivity implements StatusChangeListener
{
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		MainApplication app = (MainApplication) getApplicationContext();
		app.oMPDAsyncHelper.addStatusChangeListener(this);
	}
	
	@Override
	protected void onDestroy() 
	{
		MainApplication app = (MainApplication) getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		super.onDestroy();
	}	
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		MainApplication app = (MainApplication) getApplicationContext();
        app.setActivity(this);
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();
		MainApplication app = (MainApplication) getApplicationContext();
        app.unsetActivity(this);
	}	
	
	///////////////////////////////////////////////////////////////////////////
		
	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {}

	@Override
	public void libraryStateChanged(boolean updating) {}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {}

	@Override
	public void randomChanged(boolean random) {}

	@Override
	public void repeatChanged(boolean repeating) {}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {}
	
}
