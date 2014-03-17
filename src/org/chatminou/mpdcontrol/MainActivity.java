package org.chatminou.mpdcontrol;

import org.chatminou.mpdcontrol.mpd.MPD;
import org.chatminou.mpdcontrol.fragments.LibraryFragment;
import org.chatminou.mpdcontrol.helpers.MPDConnectionHandler;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.Fragment;
import org.chatminou.mpdcontrol.settings.MainSettingsActivity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements TabListener
{
	
	public static final int ACTIVITY_SETTINGS = 5;
			
	private ViewPager viewPager = null;
	private TabPagerAdapter pagerAdapter = null;
	private Integer currentTab = 0;
	private boolean doubleBackToExitPressedOnce = false;
	
	      	
	///////////////////////////////////////////////////////////////////////////
	
	public boolean isTablet()
	{
		if (pagerAdapter != null) return pagerAdapter.isTablet();
		return false;
	}
	
	public boolean isPortrait()
	{
		return (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
	}
	
	///////////////////////////////////////////////////////////////////////////
		
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
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		this.doubleBackToExitPressedOnce = false;
		
		MainApplication app = (MainApplication) getApplicationContext();
		MPDConnectionHandler.startReceiver(this, app.getSettingsHelper());
		
		//startService(new Intent(this, ServiceBonjour.class));
		/*try
		{
			ServiceInfo info = ServiceBonjour.discoverService(ServiceBonjour.MPD_SERVICE, 1000, this);
			if (info != null)
			{
				String name = info.getName();
				String host =info.getHostAddress();
				Log.d(MainApplication.TAG, "ServiceBonjour :" + String.format("%s, %s", name, host));
			}
			else 
			{
				Log.d(MainApplication.TAG, "ServiceBonjour : NOTHING DISCOVER");
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}*/
		 
	}
	
	@Override
	protected void onPause() 
	{
		MPDConnectionHandler.stopReceiver(this);
		
		//stopService(new Intent(this, ServiceBonjour.class));
		super.onPause();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		outState.putSerializable("currentTab", currentTab);
		super.onSaveInstanceState(outState);
	}
	
	///////////////////////////////////////////////////////////////////////////
		
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		pagerAdapter = new TabPagerAdapter(this);
		
		if (!isTablet()) setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		setContentView(pagerAdapter.getMainLayoutID());
				
		ActionBar actionBar = getActionBar();
		
		BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_actionbar_hash);
		bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		actionBar.setBackgroundDrawable(bg);
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() 
		{
			@Override
			public void onPageSelected(int position) 
			{
				getActionBar().setSelectedNavigationItem(position);
			}
		});
				
		for (int i = 0; i < pagerAdapter.getCount(); i++) 
		{
			Tab tab = actionBar.newTab();
			tab.setText(pagerAdapter.getTitle(i));
			tab.setTabListener(this);
			actionBar.addTab(tab);
		}
		
		if (savedInstanceState != null)
		{
			currentTab = (Integer) savedInstanceState.getSerializable("currentTab");
			getActionBar().setSelectedNavigationItem(currentTab);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) 
	{
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) 
	{
	}
	
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) 
	{
		if (viewPager != null)
		{
			viewPager.setCurrentItem(tab.getPosition());
			viewPager.requestLayout();
			
			currentTab = tab.getPosition();
			invalidateOptionsMenu();
					
			int id = pagerAdapter.getID(currentTab);
			if (id != -1)
			{
				android.support.v4.app.Fragment f =  getSupportFragmentManager().findFragmentById(id);
				if (f instanceof Fragment)
				{
					Fragment fragment = (Fragment) f;
					if (fragment != null) fragment.onDisplay();
				}
			}
		}
	}
	
	public int getLibraryFragmentPosition()
	{
		return pagerAdapter.getPosition(R.id.tab_library);
	}

	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onBackPressed() 
	{
		if (restoreBackFragment()) return;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean exitOnlyByMenu = settings.getBoolean(SettingsHelper.APP_EXIT_ONLY_BY_MENU, false);
		if (exitOnlyByMenu) return;
		
		if (doubleBackToExitPressedOnce)
		{
			MainApplication app = (MainApplication) getApplication();
			app.terminateApplication();
			return;
		}
		
		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, R.string.app_exit_press_back_twice_message, Toast.LENGTH_SHORT).show();
		new Handler().postDelayed(new Runnable() 
		{
			@Override public void run() 
			{
				doubleBackToExitPressedOnce = false;   
			}
		}, 2000);
	}
	
	public boolean restoreBackFragment()
	{
		/*if (this.viewPager != null)
		{
			int id = pagerAdapter.getID(currentTab);
			if (id != -1)
			{
				if (id == R.id.tab_library)
				{
					LibraryFragment fragment = (LibraryFragment) getSupportFragmentManager().findFragmentById(R.id.tab_library);
					if (fragment != null)
					{
						return fragment.restoreBackFragment();
					}
				}	
			}
		}*/
		
		return false;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        
        int id = pagerAdapter.getID(currentTab);
		if (id != -1)
		{
			if (id == R.id.tab_playingplaylist || id == R.id.tab_playlist) getMenuInflater().inflate(R.menu.playlist, menu);
		}
     
        return true;
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		Intent i = null;
		final MainApplication app = (MainApplication) this.getApplication();
		final MPD mpd = app.oMPDAsyncHelper.oMPD;
		
		switch (item.getItemId()) 
		{
			case R.id.menu_settings:
	            i = new Intent(this, MainSettingsActivity.class);
	            startActivityForResult(i, ACTIVITY_SETTINGS);
	            return true;
	            
			case R.id.menu_exit:
				app.terminateApplication();
				return true;
	    
			default:
                return super.onOptionsItemSelected(item);
                
		}
	}
	
}
