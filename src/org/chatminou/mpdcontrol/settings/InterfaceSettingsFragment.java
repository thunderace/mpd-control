package org.chatminou.mpdcontrol.settings;

import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.cover.CoverManager;
import org.chatminou.mpdcontrol.cover.provider.CachedCover;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.SettingsFragment;
import org.chatminou.mpdcontrol.tools.Tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.format.Formatter;

public class InterfaceSettingsFragment extends SettingsFragment
{
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_interface);
		
		EditTextPreference pCacheUsage = (EditTextPreference) findPreference("cover_cache_usage");
		Preference pCacheClear = (Preference) findPreference("cover_cache_clear");
		CheckBoxPreference pCoverLocal = (CheckBoxPreference) findPreference(SettingsHelper.COVER_USE_LOCAL);
		EditTextPreference pCoverLocalPath = (EditTextPreference) findPreference(SettingsHelper.COVER_USE_LOCAL_PATH);
		EditTextPreference pCoverLocalfilename = (EditTextPreference) findPreference(SettingsHelper.COVER_USE_LOCAL_FILENAME);
		init(pCacheUsage, pCacheClear, pCoverLocal, pCoverLocalPath, pCoverLocalfilename, getActivity());
	}
	
	protected static void init(EditTextPreference pCacheUsage, Preference pCacheClear, CheckBoxPreference pCoverLocal, EditTextPreference pCoverLocalPath, EditTextPreference pCoverLocalfilename, Context context)
	{
		if (pCacheUsage != null) initCacheUsage(pCacheUsage, context);
		if (pCacheClear != null && pCacheUsage != null) initCacheClear(pCacheClear, pCacheUsage, context);
		if (pCoverLocal != null && pCoverLocalfilename != null && pCoverLocalfilename != null) initCoverLocal(pCoverLocal, pCoverLocalPath, pCoverLocalfilename, context);
	}
	
	protected static void initCacheUsage(EditTextPreference pref, Context context)
	{
		MainApplication app = (MainApplication) context.getApplicationContext();
		long size = new CachedCover(app).getCacheUsage();
        String usage = Formatter.formatFileSize(app, size);
        pref.setSummary(usage);
	}
	
	protected static void initCacheClear(Preference pref, final EditTextPreference pUsage, final Context context)
	{
		final MainApplication app = (MainApplication) context.getApplicationContext();
		
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
					        	CoverManager.getInstance(app, null).clear();
							   	Tools.notifyUser(app.getResources().getString(R.string.settings_interface_covers_clear_cache_deleted), context);
							   	String usage = Formatter.formatFileSize(app, 0);
							   	pUsage.setSummary(usage);
					         	break;
				        }
				    }
				};
				
				AlertDialog.Builder ad = new AlertDialog.Builder(context);
				ad.setCancelable(true);
				ad.setTitle(R.string.settings_interface_covers_clear_cache_desc);
				ad.setMessage(R.string.settings_interface_covers_clear_cache_confirm)
					.setNegativeButton(context.getResources().getString(R.string.dialog_no), dialogClickListener)
					.setPositiveButton(context.getResources().getString(R.string.dialog_yes), dialogClickListener)
					.show();
				
				return false;
			}
		});
	}
	
	protected static void initCoverLocal(CheckBoxPreference pCoverLocal, final EditTextPreference pCoverLocalPath, final EditTextPreference pCoverLocalfilename, final Context context)
	{
		if (pCoverLocal.isChecked()) 
		{
			pCoverLocalPath.setEnabled(true);
			pCoverLocalfilename.setEnabled(true);
		} 
		else 
		{
			pCoverLocalPath.setEnabled(false);
			pCoverLocalfilename.setEnabled(false);
		}
		
		pCoverLocal.setOnPreferenceChangeListener(new OnPreferenceChangeListener() 
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) 
			{
				if ((Boolean) newValue) 
				{
					pCoverLocalPath.setEnabled(true);
					pCoverLocalfilename.setEnabled(true);
				} 
				else 
				{
					pCoverLocalPath.setEnabled(false);
					pCoverLocalfilename.setEnabled(false);
				}
				return true;
			}
		});
	}
	
}
