package org.thunder.mpdcontrol.fragments;

import java.util.List;

import org.thunder.mpdcontrol.R;
import org.thunder.actionbarpulltorefresh.ActionBarPullToRefresh;
import org.thunder.actionbarpulltorefresh.Options;
import org.thunder.actionbarpulltorefresh.PullToRefreshLayout;
import org.thunder.actionbarpulltorefresh.listeners.OnRefreshListener;
import org.thunder.mpdcontrol.activities.PlaylistEditActivity;
import org.thunder.mpdcontrol.adapters.ArrayAdapter;
import org.thunder.mpdcontrol.helpers.SettingsHelper;
import org.thunder.mpdcontrol.helpers.MPDAsyncHelper.AsyncExecListener;
import org.thunder.mpdcontrol.models.FragmentRefreshable;
import org.thunder.mpdcontrol.mpd.Item;
import org.thunder.mpdcontrol.mpd.exception.MPDServerException;
import org.thunder.mpdcontrol.tools.Tools;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistsFragment extends FragmentRefreshable implements OnMenuItemClickListener, AsyncExecListener, OnItemClickListener, OnRefreshListener 
{
	
	private static final String EXTRA_POSITION = "position";
        	
	private View view = null;
	protected AbsListView list = null;
	protected View loadingView = null;
	protected TextView loadingTextView = null;
	protected View noResultView = null;
	private PullToRefreshLayout pullToRefreshLayout = null;
	
	protected List<? extends Item> items = null;
	
	private int iJobID = -1;
	private int lastPosition = -1;
	
	public static final int ADD = 0;
	public static final int ADD_REPLACE = 2;
	public static final int ADD_REPLACE_PLAY = 3;
	public static final int ADD_PLAY = 4;
	
	public static final int EDIT = 101;
    public static final int DELETE = 102;
		
	///////////////////////////////////////////////////////////////////////////
	
	public PlaylistsFragment() 
    {
        super();
        setHasOptionsMenu(false);
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		view = inflater.inflate(R.layout.fragment_playlists, container, false);
		
	    list = (ListView) view.findViewById(R.id.list);
	    registerForContextMenu(list);
	    list.setOnItemClickListener(this);
	   
	    loadingView = view.findViewById(R.id.loadingLayout);
	    loadingTextView = (TextView) view.findViewById(R.id.loadingText);
	    loadingTextView.setText(R.string.library_loading_playlists);
	    noResultView = view.findViewById(R.id.noResultLayout);
	    	    
	    pullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pullToRefresh);
	    
	    lastPosition = -1;
		if (savedInstanceState != null) 
		{
			lastPosition = savedInstanceState.getInt(EXTRA_POSITION, 0);
		}
	
	    return view;
	}
	
	@Override
	public void onDestroyView() 
	{
	    loadingView = null;
	    loadingTextView = null;
	    noResultView = null;
	    super.onDestroyView();
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) 
    {
        super.onViewCreated(view, savedInstanceState);
        
        if (items != null) 
        {
        	list.setAdapter(getListAdapter());
        	list.setSelection(lastPosition);
        }
 
        refreshFastScrollStyle();
        
        if (pullToRefreshLayout != null) 
        {
            ActionBarPullToRefresh.from(getActivity())
            	.allChildrenArePullable()
            	.listener(this)
            	.options(Options.create().scrollDistance(0.4F).build())
            	.setup(pullToRefreshLayout);
          }
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState)
	{
		if (list != null) 
		{
			outState.putInt(EXTRA_POSITION, list.getFirstVisiblePosition());
		}
        super.onSaveInstanceState(outState);
    }
	
    ///////////////////////////////////////////////////////////////////////////
	
	@Override
    public void onRefreshStarted(View view) 
    {
        pullToRefreshLayout.setRefreshComplete();
        lastPosition = -1;
        updateList();
    }
    
    protected void refreshFastScrollStyle() 
    {
        refreshFastScrollStyle(items != null && items.size() >= SettingsHelper.MIN_ITEMS_BEFORE_FASTSCROLL);
    }

    protected void refreshFastScrollStyle(boolean shouldShowFastScroll) 
    {
    	setListViewFastScrool((ListView) list, shouldShowFastScroll);
    }
    
    ///////////////////////////////////////////////////////////////////////////
  	
	protected ListAdapter getListAdapter() 
    {
        return new ArrayAdapter(getActivity(), R.layout.playlists_item, items);
    }
	
    ///////////////////////////////////////////////////////////////////////////
	
    @Override
    public void updateList() 
    {
    	super.updateList();
    	
    	list.setAdapter(null);
    	
        noResultView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        if (pullToRefreshLayout != null) 
        {
            pullToRefreshLayout.setEnabled(false);
        }

        app.oMPDAsyncHelper.addAsyncExecListener(this);
        iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() 
        {
            @Override
            public void run() 
            {
                asyncUpdate();
            }
        });
    }
    
    @Override
    public boolean asyncExecSucceeded(int jobID) 
    {
        if (iJobID == jobID) 
        {
        	updateFromItems();
            return true;
        }
        
        return false;
    }

    protected void asyncUpdate() 
    {
    	try 
    	{
    		if (app != null) 
    		{
    			items = app.oMPDAsyncHelper.oMPD.getPlaylists(true);
    		}
    	} 
    	catch (MPDServerException e) 
    	{
    	}
    }
    
    public void updateFromItems() 
    {
        if (getView() == null) return;
        if (pullToRefreshLayout != null) pullToRefreshLayout.setEnabled(true);
        if (items != null) 
        {
        	list.setAdapter(getListAdapter());
        	list.setSelection(lastPosition);
        }
        
        try 
        {
            if (((list instanceof ListView) && ((ListView) list).getHeaderViewsCount() == 0)) 
            {
                list.setEmptyView(noResultView);
            } 
            else 
            {
                if (items == null || items.isEmpty()) 
                {
                    noResultView.setVisibility(View.VISIBLE);
                }
            }
        } 
        catch (Exception e) 
        {
        }
        
        loadingView.setVisibility(View.GONE);
        refreshFastScrollStyle();
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        int index = (int) info.id;
        if (index >= 0 && items.size() > index) 
        {
        	String name = items.get((int) info.id).toString();
        	
            menu.setHeaderTitle(name);
            android.view.MenuItem addItem = menu.add(ADD, 0, 0, getResources().getString(R.string.library_add_playlist));
            addItem.setOnMenuItemClickListener(this);
            android.view.MenuItem addAndReplaceItem = menu.add(ADD_REPLACE, 0, 0, R.string.library_add_replace);
            addAndReplaceItem.setOnMenuItemClickListener(this);
            android.view.MenuItem addAndReplacePlayItem = menu.add(ADD_REPLACE_PLAY, 0, 0, R.string.library_add_replace_play);
            addAndReplacePlayItem.setOnMenuItemClickListener(this);
            android.view.MenuItem addAndPlayItem = menu.add(ADD_PLAY, 0, 0, R.string.library_add_play);
            addAndPlayItem.setOnMenuItemClickListener(this);
            android.view.MenuItem editItem = menu.add(EDIT, 0, 0, R.string.playlists_edit);
            editItem.setOnMenuItemClickListener(this);
            if (!name.equalsIgnoreCase("Radios"))
            {
            	android.view.MenuItem deleteItem = menu.add(DELETE, 0, 0, R.string.playlists_delete);
            	deleteItem.setOnMenuItemClickListener(this);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(final android.view.MenuItem item) 
    {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final int index = (int) info.id;
        if (index >= 0 && items.size() > index) 
        {
        	final String playlist = items.get(index).getName();
	        switch (item.getGroupId()) 
	        {
	            case ADD_REPLACE_PLAY:
	            case ADD_REPLACE:
	            case ADD:
	            case ADD_PLAY:
	            {
	                app.oMPDAsyncHelper.execAsync(new Runnable() 
	                {
	                    @Override
	                    public void run() 
	                    {
	                        boolean replace = false;
	                        boolean play = false;
	                        switch (item.getGroupId()) 
	                        {
	                            case ADD_REPLACE_PLAY:
	                                replace = true;
	                                play = true;
	                                break;
	                                
	                            case ADD_REPLACE:
	                                replace = true;
	                                break;
	                                
	                            case ADD_PLAY:
	                                play = true;
	                                break;
	                        }
	                        add(items.get((int) info.id), replace, play);
	                    }
	                });
	            }
	            break;
	            
	            case EDIT:
	            {
	            	Intent intent = new Intent(getActivity(), PlaylistEditActivity.class);
	                intent.putExtra("playlist", playlist);
	                startActivity(intent);
	                return true;
	            }
	            	
	            case DELETE:
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
										app.oMPDAsyncHelper.oMPD.getPlaylist().removePlaylist(playlist);
										if (isAdded()) 
										{
											Tools.notifyUser(String.format(getResources().getString(R.string.playlists_playlist_deleted), playlist), getActivity());
										}
										items.remove(index);
										((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
									} 
									catch (MPDServerException e) 
									{
										e.printStackTrace();
										
										AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
										ad.setTitle(getResources().getString(R.string.playlists_playlist_delete));
										ad.setCancelable(false);
										ad.setMessage(String.format(getResources().getString(R.string.playlists_delete_failed), playlist))
											.setPositiveButton(getResources().getString(R.string.dialog_close), null)
											.show();
									}
						        	updateFromItems();
						            break;
					        }
					    }
					};
										
					AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
					ad.setCancelable(false);
					ad.setMessage(String.format(getResources().getString(R.string.playlists_confirm_delete), playlist))
						.setPositiveButton(getResources().getString(R.string.dialog_yes), dialogClickListener)
						.setNegativeButton(getResources().getString(R.string.dialog_no), dialogClickListener)
						.show();
	            	
	            }
	            break;	
	
	        }
        }
        return false;
    }
        
    protected void add(Item item, boolean replace, boolean play) 
    {
        try 
        {
            app.oMPDAsyncHelper.oMPD.add(item.getName(), replace, play);
            if (isAdded()) 
            {
                Tools.notifyUser(String.format(getResources().getString(R.string.library_playlist_added), item), getActivity());
            }
        } 
        catch (MPDServerException e) 
        {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		String playlist = items.get(position).getName();
		Intent intent = new Intent(getActivity(), PlaylistEditActivity.class);
        intent.putExtra("playlist", playlist);
        startActivity(intent);
	}
    
}
