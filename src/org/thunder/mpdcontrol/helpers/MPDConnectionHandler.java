package org.thunder.mpdcontrol.helpers;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class MPDConnectionHandler extends BroadcastReceiver 
{

    private static MPDConnectionHandler instance;
    private SettingsHelper settingsHelper = null;

    public static MPDConnectionHandler getInstance() 
    {
        if (instance == null) instance = new MPDConnectionHandler();
        return instance;
    }

    public static void startReceiver(Activity activity, SettingsHelper settingsHelper)
    {
    	getInstance().settingsHelper = settingsHelper;
    	activity.registerReceiver(getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
    
    public static void stopReceiver(Activity activity)
    {
    	activity.unregisterReceiver(getInstance());
    	getInstance().settingsHelper = null;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) 
    {
        String action = intent.getAction();
        
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) 
        {
        	NetworkInfo networkInfo = (NetworkInfo) intent .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        	if (networkInfo.isConnected())
        	{
        		if (settingsHelper != null)
        		{
        			settingsHelper.updateConnectionSettings();
        		}
        	}
        }
    }
    
}
