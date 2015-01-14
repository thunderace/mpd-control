package org.thunder.mpdcontrol.models;

import org.thunder.mpdcontrol.MainActivity;
import org.thunder.mpdcontrol.MainApplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.widget.AbsListView;
import android.widget.ListView;

public class ListFragment extends android.support.v4.app.ListFragment implements InterfaceFragment
{
	protected MainApplication app = null;
	protected FragmentActivity activity = null;
	protected SharedPreferences settings = null;

	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        activity = (FragmentActivity) getActivity();
        app = (MainApplication) activity.getApplication();
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
    }
	
	@Override
    public void onDestroy() 
	{
		settings = null;
		activity = null;
		app = null;
        super.onDestroy();
    }

	@Override
	public void onDisplay() 
	{
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
		if (!((MainActivity) activity).isTablet())
			style = AbsListView.SCROLLBARS_INSIDE_INSET;
		else 
			style = AbsListView.SCROLLBARS_INSIDE_OVERLAY;
		
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
