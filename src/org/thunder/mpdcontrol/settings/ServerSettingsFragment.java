package org.thunder.mpdcontrol.settings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.models.SettingsFragment;
import org.thunder.mpdcontrol.mpd.MPDOutput;
import org.thunder.mpdcontrol.mpd.exception.MPDServerException;
import org.thunder.mpdcontrol.tools.Tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class ServerSettingsFragment extends SettingsFragment
{
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_server);
		
		Preference pConn = (Preference) findPreference("server_connections");
		MultiSelectListPreference pOutputs = (MultiSelectListPreference) findPreference("server_outputs");
		Preference pUpdate = (Preference) findPreference("server_update");
		Preference pInfos = (Preference) findPreference("server_infos");
		init(pConn, pOutputs, pUpdate, pInfos, getActivity());
	}
	
	protected static void init(Preference pConn, MultiSelectListPreference pOutputs, Preference pUpdate, Preference pInfos, Context context)
	{
		if (pConn != null) initConn(pConn, context);
		if (pOutputs != null) initOutputs(pOutputs, context);
		if (pUpdate != null) initUpdate(pUpdate, context);
		if (pInfos != null) initInfos(pInfos, context);		
	}
	
	protected static void initConn(Preference pref, Context context)
	{
		Intent intent = new Intent(context, ConnectionsSettingsActivity.class);
		pref.setIntent(intent);
	}
	
	protected static void initOutputs(final MultiSelectListPreference pref, Context context)
	{
		try 
		{
			final MainApplication app = (MainApplication) context.getApplicationContext();
			if (!app.oMPDAsyncHelper.oMPD.isConnected()) 
			{
				pref.setEnabled(false);
				return;
			}
						
			final Collection<MPDOutput> list = app.oMPDAsyncHelper.oMPD.getOutputs();
			String[] entries = new String[list.size()];
			String[] entriesValues = new String[list.size()];
			Set<String> values = new HashSet<String>();
			int i = 0;
			for (MPDOutput out : list) 
			{
				entries[i] = out.getName();
				entriesValues[i] = Integer.toString(out.getId());
				if (out.isEnabled()) values.add(Integer.toString(out.getId()));
				i++;
			}
			
			pref.setEntries(entries);
			pref.setEntryValues(entriesValues);
			pref.setValues(values);
			
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() 
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) 
				{
					Set<String> values = (HashSet<String>) newValue;
					try 
					{
						for (MPDOutput out : list) 
						{
							String id = Integer.toString(out.getId());
							if (values.contains(id))
							{
								Log.d(MainApplication.TAG, "output enable " + id);
								app.oMPDAsyncHelper.oMPD.enableOutput(out.getId());
							}
							else
							{
								Log.d(MainApplication.TAG, "output disable " + id);
								app.oMPDAsyncHelper.oMPD.disableOutput(out.getId());
							}
						}
						
						Collection<MPDOutput> newlist = app.oMPDAsyncHelper.oMPD.getOutputs();
						Set<String> newvalues = new HashSet<String>();
						for (MPDOutput out : newlist) 
						{
							if (out.isEnabled())
							{
								newvalues.add(Integer.toString(out.getId()));
							}
						}
						pref.setValues(newvalues);
					}
					catch (MPDServerException e) 
					{
						e.printStackTrace();
					}	
					return false;
				}
			});
		}
		catch (MPDServerException e) 
		{
			pref.setEnabled(false);
		}	
	}
	
	protected static void initUpdate(final Preference pref, final Context context)
	{
		final MainApplication app = (MainApplication) context.getApplicationContext();
		if (!app.oMPDAsyncHelper.oMPD.isConnected()) 
		{
			pref.setEnabled(false);
			return;
		}
		
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{
			@Override
			public boolean onPreferenceClick(Preference preference) 
			{
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() 
				{
				    @Override
				    public void onClick(DialogInterface dialog, int which) 
				    {
				        switch (which)
				        {
					        case DialogInterface.BUTTON_POSITIVE:
					        	try 
					        	{
					        		app.oMPDAsyncHelper.oMPD.refreshDatabase();
					        		Tools.notifyUser(app.getResources().getString(R.string.settings_server_update_notif), context);
					         	} 
					        	catch (MPDServerException e) 
					        	{
					        		e.printStackTrace();
					        	}
					        	break;
				        }
				    }
				};
				
				AlertDialog.Builder ad = new AlertDialog.Builder(context);
				ad.setCancelable(true);
				ad.setTitle(R.string.settings_server_update_desc);
				ad.setMessage(R.string.settings_server_update_confirm)
					.setNegativeButton(context.getResources().getString(R.string.dialog_no), dialogClickListener)
					.setPositiveButton(context.getResources().getString(R.string.dialog_yes), dialogClickListener)
					.show();
				
				return false;
			}
		});
	}
	
	protected static void initInfos(final Preference pref, final Context context)
	{
		final MainApplication app = (MainApplication) context.getApplicationContext();
		if (!app.oMPDAsyncHelper.oMPD.isConnected()) 
		{
			pref.setEnabled(false);
		}
		
		final String text = getServerInfosText(app);
		if (text == null)
		{
			pref.setEnabled(false);
		}
		
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() 
		{
			@Override
			public boolean onPreferenceClick(Preference preference) 
			{
				AlertDialog.Builder ad = new AlertDialog.Builder(context);
				ad.setCancelable(true);
				ad.setTitle(R.string.settings_server_info);
				ad.setMessage(text)
					.setPositiveButton(context.getResources().getString(R.string.dialog_close), null)
					.show();
				return false;
			}
		});
	}
	
	protected static String getServerInfosText(MainApplication app)
	{
		try 
		{
			String version = app.oMPDAsyncHelper.oMPD.getMpdVersion();
			String artists = "" + app.oMPDAsyncHelper.oMPD.getStatistics().getArtists();
			String albums = "" + app.oMPDAsyncHelper.oMPD.getStatistics().getAlbums();
			String songs = "" + app.oMPDAsyncHelper.oMPD.getStatistics().getSongs();
			
			return app.getResources().getString(R.string.settings_server_infos_version) + " : " + version
					+ "\n\n" + app.getResources().getString(R.string.settings_server_infos_artists) + " : " + artists
					+ "\n\n" + app.getResources().getString(R.string.settings_server_infos_albums) + " : " + albums
					+ "\n\n" + app.getResources().getString(R.string.settings_server_infos_tracks) + " : " + songs;
		
		} 
		catch (MPDServerException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
}
