package org.chatminou.mpdcontrol.helpers;

import java.util.List;

import org.chatminou.mpdcontrol.mpd.MPD;
import org.chatminou.mpdcontrol.MainApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsHelper implements OnSharedPreferenceChangeListener
{
	public static final boolean ALWAYS_SIMPLE_PREFS = false;
	public static final int MIN_ITEMS_BEFORE_FASTSCROLL = 50;
		
	private static final int DEFAULT_MPD_PORT = 6600;
		
	public static final String SERVER_PREFIX = "server_";
	public static final String SERVER_HOSTNAME = "hostname";
	public static final String SERVER_PORT = "port";
	public static final String SERVER_PASSWORD = "password";
	
	public static final String SERVER_MPD_LOCAL_CACHE = "server.mpd_local_cache";
	
	public static final String COVER_USE_CACHE = "cover.use_cache";
	public static final String COVER_USE_LOCAL = "cover.use_local";
	public static final String COVER_USE_LOCAL_PATH = "cover.local_path";
	public static final String COVER_USE_LOCAL_FILENAME = "cover.local_filename";
	public static final String COVER_USE_INTERNET = "cover.use_internet";
	public static final String COVER_ONLY_WIFI = "cover.only_wifi";
	
	public static final String APP_EXIT_ONLY_BY_MENU = "app.exit_only_by_menu";
	public static final String APP_SHOW_STOP_BUTTON = "app.show_stop_button";
		
	public static final String ARTIST_SORT_ALBUM_BY_YEAR = "artist.sort_album_by_year";
	public static final String ALBUM_SORT_TRACK_BY_NUMBER = "album.show_sort_track_by_number";
	public static final String ALBUM_SHOW_TRACK_COUNT = "album.show_track_count";
	public static final String ALBUM_SHOW_DURATION = "album.show_duration";
	public static final String ALBUM_SHOW_YEAR = "album.show_year";
	
	private SharedPreferences settings = null;
	private MPDAsyncHelper oMPDAsyncHelper = null;
	private WifiManager mWifiManager = null;
	
	///////////////////////////////////////////////////////////////////////////
	
	public SettingsHelper(MainApplication parent, MPDAsyncHelper MPDAsyncHelper) 
	{
		settings = PreferenceManager.getDefaultSharedPreferences(parent);
		settings.registerOnSharedPreferenceChangeListener(this);
		mWifiManager = (WifiManager) parent.getSystemService(Context.WIFI_SERVICE);
		oMPDAsyncHelper = MPDAsyncHelper;
		
		setdefaultSettings();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	public void setdefaultSettings()
	{
		Editor e = settings.edit();
				
		if (!settings.contains(SERVER_MPD_LOCAL_CACHE)) e.putBoolean(SERVER_MPD_LOCAL_CACHE, false);
		
		if (!settings.contains(COVER_USE_CACHE)) e.putBoolean(COVER_USE_CACHE, true);
		if (!settings.contains(COVER_USE_LOCAL)) e.putBoolean(COVER_USE_LOCAL, true);
		if (!settings.contains(COVER_USE_LOCAL_PATH)) e.putString(COVER_USE_LOCAL_PATH, "/mpd");
		if (!settings.contains(COVER_USE_LOCAL_FILENAME)) e.putString(COVER_USE_LOCAL_FILENAME, "");
		if (!settings.contains(COVER_USE_INTERNET)) e.putBoolean(COVER_USE_INTERNET, true);
		if (!settings.contains(COVER_ONLY_WIFI)) e.putBoolean(COVER_ONLY_WIFI, true);
		
		if (!settings.contains(APP_EXIT_ONLY_BY_MENU)) e.putBoolean(APP_EXIT_ONLY_BY_MENU, false);
		if (!settings.contains(APP_SHOW_STOP_BUTTON)) e.putBoolean(APP_SHOW_STOP_BUTTON, false);
		
		if (!settings.contains(ARTIST_SORT_ALBUM_BY_YEAR)) e.putBoolean(ARTIST_SORT_ALBUM_BY_YEAR, true);
		if (!settings.contains(ALBUM_SORT_TRACK_BY_NUMBER)) e.putBoolean(ALBUM_SORT_TRACK_BY_NUMBER, true);
		if (!settings.contains(ALBUM_SHOW_YEAR)) e.putBoolean(ALBUM_SHOW_YEAR, true);
		if (!settings.contains(ALBUM_SHOW_TRACK_COUNT)) e.putBoolean(ALBUM_SHOW_TRACK_COUNT, true);
		if (!settings.contains(ALBUM_SHOW_DURATION)) e.putBoolean(ALBUM_SHOW_DURATION, true);
		
		e.commit();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	public static boolean isAndroidEmulator() 
    {
        boolean isEmulator = false;
        if (Build.PRODUCT.matches(".*_?sdk_?.*"))
        {
        	isEmulator = true;
        }
        return isEmulator;
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	private int getIntegerSetting(String name, int defaultValue) 
	{
		try 
		{
			return Integer.parseInt(settings.getString(name, Integer.toString(defaultValue)).trim());
		} 
		catch (NumberFormatException e) 
		{
			return DEFAULT_MPD_PORT;
		}
	}

	private String getStringSetting(String name) 
	{
		String value = settings.getString(name, "").trim();
		if (value.equals("")) return null;
		else return value;
	}
	
	private boolean getBooleanSetting(String name) 
	{
        return settings.getBoolean(name, false);
    }
	
	public static String getServerStringWithSSID(String param, String wifiSSID) 
	{
        if (wifiSSID == null) 
        {
        	return SERVER_PREFIX + param;
        }
        else 
        {
        	return SERVER_PREFIX + param + "_" + wifiSSID;
        }
    }

	///////////////////////////////////////////////////////////////////////////

	public static boolean isSimplePreferences(Context context) 
	{
		boolean isXLargeTablet = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
		return ALWAYS_SIMPLE_PREFS || !isXLargeTablet;
	}

	///////////////////////////////////////////////////////////////////////////
		
	private String getCurrentSSID()
	{
		return getCurrentSSID(mWifiManager);
	}
	
	public static String getCurrentSSID(WifiManager manager) 
    {
        WifiInfo info = manager.getConnectionInfo();
        String ssid = info.getSSID();
        if (ssid != null) ssid = ssid.replace("\"", "");
        
        if (ssid == null || ssid.equals("00:00:00:00:00:00") && info.getBSSID() != null)
        {
        	List<ScanResult> results = manager.getScanResults();
        	if (results != null) 
            {
            	for (ScanResult result : results) 
            	{
            		if (result.BSSID.equals(info.getBSSID()))
            		{
            			ssid = result.SSID;
            			if (ssid != null) ssid = ssid.replace("\"", "");
            			break;
            		}
            	}
            }
        }
        
        return ssid;
    }
    
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		updateSettings();		
	}
	
	public boolean updateSettings()
	{
		oMPDAsyncHelper.setUseCache(settings.getBoolean(SERVER_MPD_LOCAL_CACHE, false));
		
		MPD.setShowArtistAlbumCount(false);
		MPD.setSortAlbumsByYear(settings.getBoolean(ARTIST_SORT_ALBUM_BY_YEAR, MPD.sortAlbumsByYear()));
        
		MPD.setSortByTrackNumber(settings.getBoolean(ALBUM_SORT_TRACK_BY_NUMBER, MPD.sortByTrackNumber()));
        MPD.setShowAlbumTrackCount(settings.getBoolean(ALBUM_SHOW_TRACK_COUNT, MPD.showAlbumTrackCount()));
        
        return updateConnectionSettings();
	}
	
	public boolean updateConnectionSettings() 
    {
		String wifiSSID = getCurrentSSID();
        
        if (getStringSetting(getServerStringWithSSID(SERVER_HOSTNAME, wifiSSID)) != null) 
        {
            updateConnectionSettings(wifiSSID);
            return true;
        } 
        else if (getStringSetting(SERVER_PREFIX + SERVER_HOSTNAME) != null) 
        {
            updateConnectionSettings(null);
            return true;
        } 
        else 
        {
            return false;
        }
    }
	
	private void updateConnectionSettings(String wifiSSID) 
	{
        if (wifiSSID != null)
        {
        	if (wifiSSID.trim().equals("")) wifiSSID = null;
        }
      
        oMPDAsyncHelper.getConnectionSettings().sServer = getStringSetting(getServerStringWithSSID(SERVER_HOSTNAME, wifiSSID));
        oMPDAsyncHelper.getConnectionSettings().iPort = getIntegerSetting(getServerStringWithSSID(SERVER_PORT, wifiSSID), DEFAULT_MPD_PORT);
        oMPDAsyncHelper.getConnectionSettings().sPassword = getStringSetting(getServerStringWithSSID(SERVER_PASSWORD, wifiSSID));
    }

}
