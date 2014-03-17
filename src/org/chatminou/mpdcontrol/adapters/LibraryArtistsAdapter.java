package org.chatminou.mpdcontrol.adapters;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.chatminou.mpdcontrol.mpd.Item;
import org.chatminou.mpdcontrol.mpd.exception.MPDServerException;
import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.fragments.LibraryFragment;
import org.chatminou.mpdcontrol.models.LibraryAlbum;
import org.chatminou.mpdcontrol.models.LibraryArtist;
import org.chatminou.mpdcontrol.models.LibraryTrack;
import org.chatminou.mpdcontrol.tools.Tools;
import org.chatminou.mpdcontrol.view.AlbumExpandableListView;
import org.chatminou.mpdcontrol.view.holders.AlbumViewHolder;
import org.chatminou.mpdcontrol.view.holders.ArtistViewHolder;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class LibraryArtistsAdapter extends BaseExpandableListAdapter implements SectionIndexer
{
	
	private LibraryFragment fragment = null;
	private List<LibraryArtist> artists = null;
	
	private final Comparator localeComp = new LocaleComparator();
	private HashMap<String, Integer> alphaIndexer = null;
	private String[] sections = null;
	
	static class LocaleComparator implements Comparator 
	{
	    static final Collator defaultCollator = Collator.getInstance(Locale.getDefault());
	    public int compare(Object str1, Object str2) 
	    {
	        return defaultCollator.compare((String) str1, (String) str2);
	    }
	}
	
	///////////////////////////////////////////////////////////////////////////
		
	public LibraryArtistsAdapter(LibraryFragment fragment, List<LibraryArtist> artists)
	{
		this.fragment = fragment;
		this.artists = artists;
		init();
	}

    public void init() 
    {
        alphaIndexer = new HashMap<String, Integer>();
        
        int size = artists.size();
        int unknownPos = -1;
        for (int i = size - 1; i >= 0; i--) 
        {
        	LibraryArtist artist = artists.get(i);
        	if (artist.sortText().length() > 0) 
            {
                alphaIndexer.put(artist.sortText().substring(0, 1).toUpperCase(), i);
            } 
            else 
            {
                unknownPos = i; // save position
            }
        }

        ArrayList<String> keyList = new ArrayList<String>(alphaIndexer.keySet());
        Collections.sort(keyList, localeComp);

        // add "Unknown" at the end after sorting
        if (unknownPos >= 0) 
        {
            alphaIndexer.put("", unknownPos);
            keyList.add("");
        }

        sections = new String[keyList.size()];
        keyList.toArray(sections);
    }

	///////////////////////////////////////////////////////////////////////////
    
	@Override
	public Object getGroup(int position) 
	{
		return artists.get(position);
	}

	@Override
	public int getGroupCount()
	{
		if (artists == null) return 0;
		return artists.size();
	}

	@Override
	public long getGroupId(int position)
	{
		return artists.get(position).getID();
	}

	@Override
	public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup parent)
	{
		LibraryArtist artist = artists.get(position);
		String info = (artist.subText() == null) ? null : artist.subText();
						
		if (convertView == null) 
        {
			ArtistViewHolder holder = new ArtistViewHolder();
        	convertView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.library_item_artist, parent, false);
        	holder.artistName = (TextView) convertView.findViewById(R.id.artist_name);
        	holder.artistInfo = (TextView) convertView.findViewById(R.id.artist_info);
        	convertView.setTag(holder);
        } 
        
		ArtistViewHolder holder = (ArtistViewHolder) convertView.getTag();
        holder.artistName.setText(artist.mainText());
        if (info != null && info.length() > 0) 
        {
            holder.artistInfo.setVisibility(View.VISIBLE);
            holder.artistInfo.setText(info);
        } 
        else 
        {
            holder.artistInfo.setVisibility(View.GONE);
        }
        	
        convertView.setBackgroundResource(artist.getDrawable());
		return convertView;
	}			
		
	@Override
	public boolean hasStableIds() 
	{
		return true;
	}
				
	///////////////////////////////////////////////////////////////////////////
		
	public Object getChild(int group, int position) 
	{
		LibraryArtist artist = artists.get(group);
		if (artist == null) return null;
		return artist.getAlbum(position);
	}

	@Override
	public int getChildrenCount(int group) 
	{
		LibraryArtist artist = artists.get(group);
		if (artist == null) return 0;
		return artist.getAlbumsCount();
	}
	
	@Override
	public long getChildId(int group, int position) 
	{
		LibraryArtist artist = artists.get(group);
		if (artist == null) return 0;
		return artist.getAlbum(position).getID();
	}
		
	@Override
	public View getChildView(final int group, final int position, boolean isExpanded, View convertView, ViewGroup parent) 
	{
		LibraryArtist artist = artists.get(group);
		LibraryAlbum album = artist.getAlbum(position);
		if (album.getAdapter() == null) album.setAdapter(new LibraryAlbumAdapter(fragment, album));
		if (album.getList() == null) album.setList(new AlbumExpandableListView(fragment.getActivity()));
		
		AlbumExpandableListView list = album.getList();
		list.setBackgroundResource(album.getDrawable());
		list.setAdapter(album.getAdapter());
		list.setGroupIndicator(null);
		setupList(list, album);
		if (album.isExpanded()) list.expandGroup(0);
		return list;
	}
	
	@Override
	public boolean isChildSelectable(int arg0, int arg1) 
	{
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public int getPositionForSection(int sectionIndex) 
	{
		String letter = sections[sectionIndex >= sections.length ? sections.length - 1 : sectionIndex];
        return alphaIndexer.get(letter);
	}

	@Override
	public int getSectionForPosition(int position) 
	{
		if (sections.length == 0) return -1;
        if (sections.length == 1) return 1;
        for (int i = 0; i < (sections.length - 1); i++) 
        {
            int begin = alphaIndexer.get(sections[i]);
            int end = alphaIndexer.get(sections[i + 1]) - 1;
            if (position >= begin && position <= end) return i;
        }
        return sections.length - 1;
	}

	@Override
	public Object[] getSections() 
	{
		return sections;
	}
	    
	///////////////////////////////////////////////////////////////////////////
	
	private void setupList(final AlbumExpandableListView list, final LibraryAlbum album)
	{
		list.setOnGroupClickListener(new OnGroupClickListener() 
		{
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) 
            {
            	if (list.isGroupExpanded(groupPosition))
            	{
            		album.setExpanded(false);
	            	list.collapseGroup(groupPosition);
	            	return true;
            	}
            	else
            	{
            		if (album == null) return true;
	            	
	            	if (album.getTracks() == null)
	            	{
	            		try 
	        			{
	            			album.initTracks(fragment.getApplication().oMPDAsyncHelper.oMPD.getSongs(album));
						} 
	        			catch (MPDServerException e) 
	        			{
	        				album.initTracks(null);
							e.printStackTrace();
						}
	            	}
	            	
	            	album.setExpanded(true);
	            	list.expandGroup(groupPosition);
	            	return true;
            	}
            }
        });
	
		list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() 
		{
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
            {
            	ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        		int index = (int) info.id;
        		if (index == -1) return;
        		
        		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
            	int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            	int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
            	
            	if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) 
            	{
            		menu.setHeaderTitle(album.toString());
            		MenuItem addItem = menu.add(LibraryFragment.ADD, album.getID(), 0, fragment.getResources().getString(R.string.library_add_album));
            		addItem.setOnMenuItemClickListener(fragment);
            		MenuItem addAndReplaceItem = menu.add(LibraryFragment.ADD_REPLACE, album.getID(), 0, R.string.library_add_replace);
                    addAndReplaceItem.setOnMenuItemClickListener(fragment);
                    MenuItem addAndReplacePlayItem = menu.add(LibraryFragment.ADD_REPLACE_PLAY, album.getID(), 0, R.string.library_add_replace_play);
                    addAndReplacePlayItem.setOnMenuItemClickListener(fragment);
                    MenuItem addAndPlayItem = menu.add(LibraryFragment.ADD_PLAY, album.getID(), 0, R.string.library_add_play);
                    addAndPlayItem.setOnMenuItemClickListener(fragment);
                    
                    int id = 0;
                    SubMenu playlistMenu = menu.addSubMenu(R.string.library_add_to_playlist);
                    android.view.MenuItem item = playlistMenu.add(LibraryFragment.ADD_TO_PLAYLIST, album.getID(), id++, R.string.library_new_playlist);
                    item.setOnMenuItemClickListener(fragment);
                    try 
                    {
                        List<Item> playlists = ((MainApplication) fragment.getActivity().getApplication()).oMPDAsyncHelper.oMPD.getPlaylists();
                        if (null != playlists)
                        {
                            for (Item pl : playlists) 
                            {
                                item = playlistMenu.add(LibraryFragment.ADD_TO_PLAYLIST, album.getID(), id++, pl.getName());
                                item.setOnMenuItemClickListener(fragment);
                            }
                        }
                    } 
                    catch (MPDServerException e) 
                    {
                        e.printStackTrace();
                    }
            	}
            	else
            	{
            		LibraryTrack track = album.getTrack(child);
            		menu.setHeaderTitle(track.getMusic().getTitle());
            		android.view.MenuItem addItem = menu.add(LibraryFragment.ADD, track.getID(), 0, fragment.getResources().getString(R.string.library_add_track));
            		addItem.setOnMenuItemClickListener(fragment);
            		android.view.MenuItem addAndReplaceItem = menu.add(LibraryFragment.ADD_REPLACE, track.getID(), 0, R.string.library_add_replace);
                    addAndReplaceItem.setOnMenuItemClickListener(fragment);
                    android.view.MenuItem addAndReplacePlayItem = menu.add(LibraryFragment.ADD_REPLACE_PLAY, track.getID(), 0, R.string.library_add_replace_play);
                    addAndReplacePlayItem.setOnMenuItemClickListener(fragment);
                    android.view.MenuItem addAndPlayItem = menu.add(LibraryFragment.ADD_PLAY, track.getID(), 0, R.string.library_add_play);
                    addAndPlayItem.setOnMenuItemClickListener(fragment);
                    
                    int id = 0;
                    SubMenu playlistMenu = menu.addSubMenu(R.string.library_add_to_playlist);
                    android.view.MenuItem item = playlistMenu.add(LibraryFragment.ADD_TO_PLAYLIST, track.getID(), id++, R.string.library_new_playlist);
                    item.setOnMenuItemClickListener(fragment);
                    try 
                    {
                        List<Item> playlists = ((MainApplication) fragment.getActivity().getApplication()).oMPDAsyncHelper.oMPD.getPlaylists();
                        if (null != playlists)
                        {
                            for (Item pl : playlists) 
                            {
                                item = playlistMenu.add(LibraryFragment.ADD_TO_PLAYLIST, track.getID(), id++, pl.getName());
                                item.setOnMenuItemClickListener(fragment);
                            }
                        }
                    } 
                    catch (MPDServerException e) 
                    {
                        e.printStackTrace();
                    }
        	   	}
            }
		});  
	
		list.setOnChildClickListener(new OnChildClickListener() 
		{
            @Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition , long id) 
			{
            	if (album != null)
            	{
            		final LibraryTrack track = album.getTrack(childPosition);
            		if (track != null)
            		{
            			fragment.getApplication().oMPDAsyncHelper.execAsync(new Runnable()
            			{
            				@Override
            				public void run() 
            				{
		            			try
		            			{
		            				fragment.getApplication().oMPDAsyncHelper.oMPD.add(track.getMusic(), false, false);
		            				if (fragment.isAdded()) Tools.notifyUser(String.format(fragment.getResources().getString(R.string.library_track_added), track.getMusic()), fragment.getActivity());
            					}
            					catch (MPDServerException e) 
            					{
            						e.printStackTrace();
            					}
            				}
        				});
            		}
            	}
				return false;
			}
        });
	}
	
}

///////////////////////////////////////////////////////////////////////////

