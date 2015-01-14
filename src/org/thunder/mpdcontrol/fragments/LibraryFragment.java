package org.thunder.mpdcontrol.fragments;

import java.util.ArrayList;
import java.util.List;

import org.thunder.mpdcontrol.R;
import org.thunder.actionbarpulltorefresh.ActionBarPullToRefresh;
import org.thunder.actionbarpulltorefresh.Options;
import org.thunder.actionbarpulltorefresh.PullToRefreshLayout;
import org.thunder.actionbarpulltorefresh.listeners.OnRefreshListener;
import org.thunder.mpdcontrol.adapters.LibraryArtistsAdapter;
import org.thunder.mpdcontrol.helpers.SettingsHelper;
import org.thunder.mpdcontrol.helpers.MPDAsyncHelper.AsyncExecListener;
import org.thunder.mpdcontrol.models.FragmentRefreshable;
import org.thunder.mpdcontrol.models.LibraryAlbum;
import org.thunder.mpdcontrol.models.LibraryArtist;
import org.thunder.mpdcontrol.models.LibraryTrack;
import org.thunder.mpdcontrol.mpd.Album;
import org.thunder.mpdcontrol.mpd.Artist;
import org.thunder.mpdcontrol.mpd.Item;
import org.thunder.mpdcontrol.mpd.exception.MPDServerException;
import org.thunder.mpdcontrol.tools.Tools;
import org.thunder.mpdcontrol.view.AlbumExpandableListView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class LibraryFragment extends FragmentRefreshable implements OnSharedPreferenceChangeListener, OnRefreshListener, AsyncExecListener, OnMenuItemClickListener
{
	
	private static final String EXTRA_POSITION = "position";
    
	private View view = null;
	protected ExpandableListView list = null;
	protected View loadingView = null;
	protected TextView loadingTextView = null;
	protected View noResultView = null;
	private PullToRefreshLayout pullToRefreshLayout = null;
	
	protected List<LibraryArtist> artists = null;
		
	private int iJobID = -1;
	private int lastPosition = -1;
	private Artist artistToDisplay = null;
	private Album albumToDisplay = null;
		
	public static final int ADD = 20;
	public static final int ADD_REPLACE = 21;
	public static final int ADD_REPLACE_PLAY = 22;
	public static final int ADD_PLAY = 23;
	public static final int ADD_TO_PLAYLIST = 24;
	
	///////////////////////////////////////////////////////////////////////////
	
	public LibraryFragment() 
    {
		super();
		setHasOptionsMenu(false);
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	getSettings().registerOnSharedPreferenceChangeListener(this);
    }
	
	@Override
    public void onDestroy() 
	{
		getSettings().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		view = inflater.inflate(R.layout.fragment_library, container, false);
		
		list = (ExpandableListView) view.findViewById(R.id.list);
	  
		list.setOnGroupClickListener(new OnGroupClickListener() 
		{
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) 
            {
            	if (list.isGroupExpanded(groupPosition))
            	{
            		return false;
            	}
            	else
            	{
            		if (artists == null) return true;
	            	LibraryArtist artist = artists.get(groupPosition);
	            	if (artist == null) return true;
	            	
	            	if (artist.getAlbums() == null)
	            	{
	            		try 
	        			{
	            			artist.initAlbums(app.oMPDAsyncHelper.oMPD.getAlbums(artist, true));
						} 
	        			catch (MPDServerException e) 
	        			{
	        				artist.initAlbums(null);
							e.printStackTrace();
						}
	            	}
	            	
	            	if (albumToDisplay != null)
	        		{
	        			privateDisplayAlbum(albumToDisplay, artist, groupPosition);
	        			albumToDisplay = null;
	        		}
	            	
	            	return false;
            	}
            }
        });
				
		loadingView = view.findViewById(R.id.loadingLayout);
	    loadingTextView = (TextView) view.findViewById(R.id.loadingText);
	    loadingTextView.setText(R.string.library_loading_artists);
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
        
        if (artists != null) 
        {
        	list.setAdapter(getArtistAdapter());
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		if (key.equals(SettingsHelper.COVER_USE_CACHE) ||
				key.equals(SettingsHelper.ARTIST_SORT_ALBUM_BY_YEAR) || 
				key.equals(SettingsHelper.ALBUM_SHOW_YEAR) ||
				key.equals(SettingsHelper.ALBUM_SHOW_TRACK_COUNT) ||
				key.equals(SettingsHelper.ALBUM_SHOW_DURATION)) 
			{
				lastPosition = list.getFirstVisiblePosition();
				invalidateRefresh();
			}
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
        refreshFastScrollStyle(artists != null && artists.size() >= SettingsHelper.MIN_ITEMS_BEFORE_FASTSCROLL);
    }

    protected void refreshFastScrollStyle(boolean shouldShowFastScroll) 
    {
    	setListViewFastScrool((ListView) list, shouldShowFastScroll);
    }
    
    ///////////////////////////////////////////////////////////////////////////
        
	protected LibraryArtistsAdapter getArtistAdapter() 
    {
		return new LibraryArtistsAdapter(this, artists);
    }
		
	///////////////////////////////////////////////////////////////////////////
    	
	@Override
    public void updateList() 
    {
    	super.updateList();
    	
    	list.setAdapter(new BaseExpandableListAdapter() {
			
			@Override public boolean isChildSelectable(int groupPosition, int childPosition) { return false; }
			@Override public boolean hasStableIds() { return false; }
			@Override public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {  return null; }
			@Override public long getGroupId(int groupPosition) { return 0; }
			@Override public int getGroupCount() { return 0; }
			@Override public Object getGroup(int groupPosition) { return null; }
			@Override public int getChildrenCount(int groupPosition) { return 0; }
			@Override public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) { return null; }
			@Override public long getChildId(int groupPosition, int childPosition) { return 0; }
			@Override public Object getChild(int groupPosition, int childPosition) { return null; }
		});
    	
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
        	int ID = 1;
        	List<? extends Item> items = app.oMPDAsyncHelper.oMPD.getArtists();
            if (items != null)
            {
            	List<LibraryArtist> artistsTmp = new ArrayList<LibraryArtist>();
            	for (Item a : items)
            	{
            		LibraryArtist artist = new LibraryArtist((Artist) a, 1000000*ID);
            		artist.setDrawable((ID % 2 == 0) ? R.drawable.list_view_item_color1 : R.drawable.list_view_item_color2);
            		artistsTmp.add(artist);
            		ID++;
            	}
            	artists = artistsTmp;
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
        if (artists != null) 
        {
        	list.setAdapter(getArtistAdapter());
        	
        	if (artistToDisplay != null)
    		{
    			privateDisplayArtist(artistToDisplay);
    			artistToDisplay = null;
    		}
    		else
    		{
    			list.setSelection(lastPosition);
    		}
        }
        
        try 
        {
            if (((list instanceof ListView) && ((ListView) list).getHeaderViewsCount() == 0)) 
            {
                list.setEmptyView(noResultView);
            } 
            else 
            {
                if (artists == null || artists.isEmpty()) 
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
	public boolean onMenuItemClick(final MenuItem item) 
	{		
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
                            case ADD_REPLACE_PLAY: replace = true; play = true; break;
                            case ADD_REPLACE: replace = true; break;
                            case ADD_PLAY: play = true; break;
                        }
                        add(item.getItemId(), replace, play);
                    }
                });
            }
            break;
                        
            case ADD_TO_PLAYLIST: 
            {
            	final EditText input = new EditText(getActivity());
                if (item.getOrder() == 0) 
                {
                    new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.library_playlist_name)
                        .setMessage(R.string.library_new_playlist_prompt)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
	                    {
	                        	public void onClick(DialogInterface dialog, int whichButton) 
	                        	{
	                        		final String name = input.getText().toString().trim();
	                        		if (null != name && name.length() > 0) 
	                        		{
	                        			app.oMPDAsyncHelper.execAsync(new Runnable()
	                        			{
	                        				@Override
	                        				public void run() 
	                        				{
	                        					add(item.getItemId(), name);
	                        				}
	                        			});
	                        		}
	                        	}
	                    })
	                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() 
	                    {
	                    	public void onClick(DialogInterface dialog, int whichButton) 
                            {
                            }
                        })
                        .show();
                } 
                else 
                {
                    add(item.getItemId(), item.getTitle().toString());
                }
            }
            break;
		}
		
		return false;
	}
	
	
	private void add(int ID, boolean replace, boolean play)
	{
		add(ID, replace, play, null);
	}
	
	private void add(int ID, String playlist)
	{
		add(ID, false, false, playlist);
	}
	
	private void add(int ID, boolean replace, boolean play, String playlist) 
    {
		if (ID == 0) return;
		
		int ID_artist = (ID / 1000000) - 1;
		ID = ID % 1000000;
		int ID_album = (ID / 1000) - 1;
		ID = ID % 1000;
		int ID_track = ID - 1;
		
		try 
		{
			if (ID_track != -1) // track
			{
				LibraryArtist artist = artists.get(ID_artist);
				LibraryAlbum album = artist.getAlbum(ID_album);
				LibraryTrack track = album.getTrack(ID_track);
				if (playlist != null) 
				{
					PlaylistsFragment f = (PlaylistsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.tab_playlists);
			    	if (f != null) f.invalidateRefresh();
			    	app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, track.getMusic());
				}
				else app.oMPDAsyncHelper.oMPD.add(track.getMusic(), replace, play);
				if (isAdded()) Tools.notifyUser(String.format(getResources().getString(R.string.library_track_added), track.getMusic()), getActivity());
			}
			else if (ID_album != -1) // album
			{
				LibraryArtist artist = artists.get(ID_artist);
				LibraryAlbum album = artist.getAlbum(ID_album);
				if (playlist != null) app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, album);
				else app.oMPDAsyncHelper.oMPD.add(album, replace, play);
				if (isAdded()) Tools.notifyUser(String.format(getResources().getString(R.string.library_album_added), album), getActivity());
			}
			else  if (ID_artist != -1) // artist
			{
				LibraryArtist artist = artists.get(ID_artist);
				if (playlist != null) app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, artist);
				else app.oMPDAsyncHelper.oMPD.add(artist, replace, play);
				if (isAdded()) Tools.notifyUser(String.format(getResources().getString(R.string.library_artist_added), artist), getActivity());
			}
    	}
		catch (MPDServerException e) 
		{
			e.printStackTrace();
		}
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	void displayArtist(final Artist artist)
	{
		if (artists == null)
		{
			artistToDisplay = artist;
		}
		else
		{
			activity.runOnUiThread(new Runnable() 
			{
			    @Override
			    public void run() 
			    {
			    	privateDisplayArtist(artist);
			    }
			});
			
		}
	}
	
	void displayAlbum(Album album)
	{
		albumToDisplay = album;
		displayArtist(album.getArtist());
	}
	
	///////////////////////////////////////////////////////////////////////////
		
	LibraryArtist privateDisplayArtist(Artist artist)
	{
		LibraryArtist artistFound = null;
		int indexFound = -1;
		
		for (int i = 0; i < list.getCount(); i++)
		{
			LibraryArtist a = (LibraryArtist) list.getItemAtPosition(i);
			if (a != null && a instanceof LibraryArtist && a.nameEquals(artist))
			{
				indexFound = i;
				artistFound = a;
				break;
			}
		}
		if (artistFound == null) return null;
				
		list.setSelection(indexFound);
		if (!list.isGroupExpanded(artists.indexOf(artistFound)))
		{
			list.performItemClick(list.getChildAt(indexFound), indexFound, artistFound.getID());
		}
		else
		{
			if (albumToDisplay != null) 
			{
				privateDisplayAlbum(albumToDisplay, artistFound, indexFound);
				albumToDisplay = null;
			}
		}
		return artistFound;
	}
		
	void privateDisplayAlbum(Album album, LibraryArtist artist, int position)
	{
		for (LibraryAlbum al : artist.getAlbums()) 
        {
			if (al.nameEquals(album))
			{
				if (al.getTracks() == null)
            	{
            		try 
        			{
            			al.initTracks(getApplication().oMPDAsyncHelper.oMPD.getSongs(al));
					} 
        			catch (MPDServerException e) 
        			{
        				al.initTracks(null);
						e.printStackTrace();
					}
            	}
				
				al.setExpanded(true);
				
				AlbumExpandableListView l = al.getList();
				if (l != null) 
				{
					l.expandGroup(0);
				}				
				
				break;
			}
        }
	}
	
	///////////////////////////////////////////////////////////////////////////

}