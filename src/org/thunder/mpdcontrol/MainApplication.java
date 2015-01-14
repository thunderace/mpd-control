package org.thunder.mpdcontrol;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.helpers.MPDAsyncHelper;
import org.thunder.mpdcontrol.helpers.RadioStore;
import org.thunder.mpdcontrol.helpers.SettingsHelper;
import org.thunder.mpdcontrol.helpers.MPDAsyncHelper.ConnectionListener;
import org.thunder.mpdcontrol.mpd.MPD;
import org.thunder.mpdcontrol.mpd.MPDStatus;
import org.thunder.mpdcontrol.settings.ConnectionSettingsActivity;
import org.thunder.mpdcontrol.settings.ConnectionsSettingsActivity;
import org.thunder.mpdcontrol.settings.MainSettingsActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;

public class MainApplication extends Application implements ConnectionListener 
{
	public static final String TAG = "mpd-control";
	
	private static final long DISCONNECT_TIMER = 15000; 
	
	public MPDAsyncHelper oMPDAsyncHelper = null;
	private SettingsHelper settingsHelper = null;
	private ApplicationState state = new ApplicationState();
	private Collection<Object> connectionLocks = new LinkedList<Object>();
	private AlertDialog ad;
	private Activity currentActivity;
	private Timer disconnectSheduler;
	private RadioStore radioStore = RadioStore.getInstance();
		
	public class ApplicationState 
	{
		public boolean streamingMode = false;
		public boolean settingsShown = false;
		public MPDStatus currentMpdStatus = null;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	class DialogClickListener implements OnClickListener 
	{
		public void onClick(DialogInterface dialog, int which) 
		{
			dismissAlertDialog();
			switch (which) 
			{
				case AlertDialog.BUTTON_NEUTRAL:
					Intent i = new Intent(currentActivity, ConnectionsSettingsActivity.class);
					currentActivity.startActivityForResult(i, 0);
					break;
			
				case AlertDialog.BUTTON_NEGATIVE:
					currentActivity.finish();
					break;
					
				case AlertDialog.BUTTON_POSITIVE:
					connectMPD();
					break;
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		MPD.setApplicationContext(getApplicationContext());

		StrictMode.VmPolicy vmpolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		StrictMode.setVmPolicy(vmpolicy);
		
		oMPDAsyncHelper = new MPDAsyncHelper();
		oMPDAsyncHelper.addConnectionListener((MainApplication) getApplicationContext());
		
		settingsHelper = new SettingsHelper(this, oMPDAsyncHelper);
		
		radioStore.init();
		
		disconnectSheduler = new Timer();
	}
			
	public void setActivity(Object activity) 
	{
		if (activity instanceof Activity) currentActivity = (Activity) activity;
	    connectionLocks.add(activity);
        checkConnectionNeeded();
        cancelDisconnectSheduler();
	}

	public void unsetActivity(Object activity) 
	{
		connectionLocks.remove(activity);
		if (currentActivity == activity) currentActivity = null;
	}
		
	public ApplicationState getApplicationState() 
	{
		return state;
	}
	
	public SettingsHelper getSettingsHelper() 
	{
		return settingsHelper;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private void checkConnectionNeeded()
	{
		if (connectionLocks.size() > 0) 
        {
            if (!oMPDAsyncHelper.isMonitorAlive()) 
            {
                oMPDAsyncHelper.startMonitor();
            }
           
            if (!oMPDAsyncHelper.oMPD.isConnected())
            {
            	if (currentActivity == null || !(currentActivity.getClass().equals(ConnectionsSettingsActivity.class) || currentActivity.getClass().equals(ConnectionSettingsActivity.class)))
            	{
            		connect();
            	}
            }
        } 
		else 
		{
            disconnect();
        }
    }
	
	
	public void terminateApplication() 
	{
		//stopService(new Intent(this.currentActivity, ServiceBonjour.class));
		currentActivity.finish();
		System.exit(0);
	}
	
	///////////////////////////////////////////////////////////////////////////

	public void connect() 
	{
		if (!settingsHelper.updateSettings())
		{
            if (currentActivity != null && !state.settingsShown) 
            {
                currentActivity.startActivityForResult(new Intent(currentActivity, ConnectionsSettingsActivity.class), MainActivity.ACTIVITY_SETTINGS);
                state.settingsShown = true;
                return;
            }
        }

		connectMPD();
	}
		
	public void disconnect() 
	{
		cancelDisconnectSheduler();
		startDisconnectSheduler();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private void startDisconnectSheduler() 
	{
		disconnectSheduler.schedule(new TimerTask() 
		{
			@Override
			public void run() 
			{
				oMPDAsyncHelper.stopMonitor();
				oMPDAsyncHelper.disconnect();
			}
		}, DISCONNECT_TIMER);
	}
	
	void cancelDisconnectSheduler() 
	{
		disconnectSheduler.cancel();
		disconnectSheduler.purge();
		disconnectSheduler = new Timer();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private void connectMPD() 
	{
		dismissAlertDialog();
		
		if (currentActivity != null) 
		{
            ad = new ProgressDialog(currentActivity);
            ad.setTitle(getResources().getString(R.string.app_connecting));
            ad.setMessage(getResources().getString(R.string.app_connecting_to_server));
            ad.setCancelable(false);
            ad.setOnKeyListener(new OnKeyListener() 
            {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) 
                {
                    return true;
                }
            });
            
            try 
            {
                ad.show();
            }
            catch (BadTokenException e)
            {
            }
        }
		
		cancelDisconnectSheduler();
		
		oMPDAsyncHelper.connect();
	}

	public void connectionFailed(String message) 
	{
		if (ad != null && !(ad instanceof ProgressDialog) && ad.isShowing()) return;
       
		dismissAlertDialog();
        oMPDAsyncHelper.disconnect();
       
        if (currentActivity == null) return;
        
        if (currentActivity != null && connectionLocks.size() > 0) 
        {
            if (currentActivity.getClass() == MainSettingsActivity.class) 
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                builder.setCancelable(false);
                builder.setMessage(String.format( getResources().getString(R.string.app_connection_failed_message_setting), message));
                builder.setPositiveButton("OK", new OnClickListener() 
                {
                    public void onClick(DialogInterface arg0, int arg1) 
                    {
                    }
                });
                
                ad = builder.show();
            } 
            else if (!(currentActivity.getClass().equals(ConnectionsSettingsActivity.class) || currentActivity.getClass().equals(ConnectionSettingsActivity.class)))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                builder.setTitle(getResources().getString(R.string.app_connection_failed));
                builder.setMessage(String.format( getResources().getString(R.string.app_connection_failed_message), message));
                builder.setCancelable(false);

                DialogClickListener oDialogClickListener = new DialogClickListener();
                builder.setNegativeButton(getResources().getString(R.string.dialog_quit), oDialogClickListener);
                builder.setNeutralButton(getResources().getString(R.string.menu_settings), oDialogClickListener);
                builder.setPositiveButton(getResources().getString(R.string.dialog_retry), oDialogClickListener);
                try 
                {
                    ad = builder.show();
                } 
                catch (BadTokenException e) 
                {
                }
            }
        }
	}

	public void connectionSucceeded(String message) 
	{
		dismissAlertDialog();
	}

	///////////////////////////////////////////////////////////////////////////

	private void dismissAlertDialog() 
	{
		if (ad != null) 
		{
			if (ad.isShowing()) 
			{
				try 
				{
					ad.dismiss();
				} 
				catch (IllegalArgumentException e) 
				{
				}
			}
		}
		ad = null;
	}
	
	///////////////////////////////////////////////////////////////////////////

}

