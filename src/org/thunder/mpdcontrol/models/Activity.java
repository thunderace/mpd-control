package org.thunder.mpdcontrol.models;

import org.thunder.mpdcontrol.MainActivity;
import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.TabPagerAdapter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.AbsListView;
import android.widget.ListView;

public abstract class Activity extends android.app.Activity
{
	protected MainApplication app = null;
	private SharedPreferences settings = null;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		app = (MainApplication) getApplication();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	@Override
	protected void onDestroy() 
	{
		settings = null;
		app = null;
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
	
	public Activity getThis()
	{
		return this;
	}
	
	public SharedPreferences getSettings() 
	{
		return settings;
	}
	
	protected void setListViewFastScrool(ListView list, boolean visible)
	{
		// Note : setting the scrollbar style before setting the
        // fastscroll state is very important pre-KitKat, because of
        // a bug.
        // It is also very important post-KitKat because it needs
        // the opposite order or it won't show the FastScroll
        // This is so stupid I don't even .... argh
		
		int style;
		TabPagerAdapter adapter = new TabPagerAdapter(this);
		if (!adapter.isTablet())
			style = AbsListView.SCROLLBARS_INSIDE_OVERLAY;
		else 
			style = AbsListView.SCROLLBARS_INSIDE_INSET;
		
		if (visible)
		{
		    if (android.os.Build.VERSION.SDK_INT >= 19) 
            {
                list.setFastScrollAlwaysVisible(true);
                list.setScrollBarStyle(style);
            } 
            else 
            {
                list.setScrollBarStyle(style);
                list.setFastScrollAlwaysVisible(true);
            }
        } 
        else 
        {
            if (android.os.Build.VERSION.SDK_INT >= 19) 
            {
                list.setFastScrollAlwaysVisible(false);
                list.setScrollBarStyle(style);
            } 
            else 
            {
                list.setScrollBarStyle(style);
                list.setFastScrollAlwaysVisible(false);
            }
        }
	}

}
