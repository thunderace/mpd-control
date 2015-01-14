package org.thunder.mpdcontrol;

import org.thunder.mpdcontrol.R;

import android.app.Activity;
import android.content.res.Configuration;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

public class TabPagerAdapter extends PagerAdapter 
{

	enum screenType
	{
		STANDARD,
		TABLET_7,
		TABLET_10		
	}
	
	private Activity activity = null;
	private screenType screen = screenType.STANDARD;
	private int layoutID;
	private int tabID[] = null;
	private int stringID[] = null;
	private int count = 0;
	
	public TabPagerAdapter(Activity activity)
	{
		super();
		this.activity = activity;
		
		double screenInches = getScreenInches();
		int screenSize = activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		
		if (screenSize < Configuration.SCREENLAYOUT_SIZE_LARGE) screen = screenType.STANDARD;
		else if (screenSize >= Configuration.SCREENLAYOUT_SIZE_XLARGE || screenInches >= 9.5f) screen = screenType.TABLET_10;
		else screen = screenType.TABLET_7;

		//screen = screenType.STANDARD;
		
		switch (screen)
		{
			case STANDARD: setupStandard(); break;
			case TABLET_7: setupTablet7();  break;
			case TABLET_10: setupTablet10(); break;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////

	public screenType getScreenType()
	{
		return screen;
	}
	
	public boolean isTablet()
	{
		return !(screen == screenType.STANDARD);
	}
	
	public int getMainLayoutID()
	{
		return layoutID;
	}
	
	protected double getScreenInches()
	{
		DisplayMetrics dm = activity.getResources().getDisplayMetrics();
		double density = dm.density * 160;
		double x = Math.pow(dm.widthPixels / density, 2);
		double y = Math.pow(dm.heightPixels / density, 2);
		double screenInches = Math.sqrt(x + y);
		return screenInches; 
	}	
	
	///////////////////////////////////////////////////////////////////////////

	private void setupStandard()
	{
		layoutID = R.layout.activity_main; 
		
		count = 0;
		tabID = new int[4];							stringID = new int[4];
		tabID[count] = R.id.tab_playing;			stringID[count++] = R.string.tab_playing;	
		tabID[count] = R.id.tab_playlist;			stringID[count++] = R.string.tab_playlist;	
		tabID[count] = R.id.tab_library;			stringID[count++] = R.string.tab_library;	
		tabID[count] = R.id.tab_playlists;			stringID[count++] = R.string.tab_playlists;	
	}
	
	private void setupTablet7()
	{
		layoutID = R.layout.activity_main_tablet; 
		
		count = 0;
		tabID = new int[3];							stringID = new int[3];
		tabID[count] = R.id.tab_playingplaylist;	stringID[count++] = R.string.tab_playingplaylist;	
		tabID[count] = R.id.tab_library;			stringID[count++] = R.string.tab_library;	
		tabID[count] = R.id.tab_playlists;			stringID[count++] = R.string.tab_playlists;	
	}
	
	private void setupTablet10()
	{
		layoutID = R.layout.activity_main_tablet; 
		
		count = 0;
		tabID = new int[3];							stringID = new int[3];
		tabID[count] = R.id.tab_playingplaylist;	stringID[count++] = R.string.tab_playingplaylist;	
		tabID[count] = R.id.tab_library;			stringID[count++] = R.string.tab_library;	
		tabID[count] = R.id.tab_playlists;			stringID[count++] = R.string.tab_playlists;	
	}

	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public int getCount()
	{
		return count;
	}
		
	@Override
	public boolean isViewFromObject(View arg0, Object arg1) 
	{
		return arg0 == ((View) arg1);
	}
	
	@Override
	public Object instantiateItem(View container, int position) 
	{
		return activity.findViewById(tabID[position]);
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) 
	{
        return;
    }

	public int getID(int position) 
	{
		if (position < 0 || position >= count) return -1;
    	return tabID[position];
	}
	
	public String getTitle(int position) 
	{
		if (position < 0 || position >= count) return null;
    	return activity.getString(stringID[position]);
	}
	
	public int getPosition(int id)
	{
		for (int position = 0; position < count; position++) 
		{
			if (tabID[position] == id) return position; 
		}
		return -1;
	}
	
	///////////////////////////////////////////////////////////////////////////
		
}