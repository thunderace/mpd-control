package org.chatminou.mpdcontrol.models;

import java.util.ArrayList;
import java.util.List;

import org.chatminou.mpdcontrol.mpd.Album;
import org.chatminou.mpdcontrol.mpd.Item;
import org.chatminou.mpdcontrol.mpd.Music;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.adapters.LibraryAlbumAdapter;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.view.AlbumExpandableListView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

public class LibraryAlbum extends Album 
{

	private int ID = -1;
	private int drawable = -1;
	private List<LibraryTrack> tracks = null;
	private LibraryAlbumAdapter adapter = null;
	private Drawable drawableCover = null;
	private AlbumExpandableListView list = null;
	private boolean isExpanded = false;
	
	public LibraryAlbum(Album a) 
	{
		super(a);
		this.ID = -1;
		this.drawable = -1;
		this.tracks = null;
		this.adapter = null;
		this.drawableCover = null;
		this.list = null;
		this.isExpanded = false;
	}
	
	public LibraryAlbum(Album a, int ID) 
	{
		super(a);
		this.ID = ID;
		this.drawable = -1;
		this.tracks = null;
		this.adapter = null;
		this.drawableCover = null;
		this.list = null;
		this.isExpanded = false;
	}
	
	public int getID()
	{
		return ID;
	}
	
	public void setDrawable(int drawable)
	{
		this.drawable = drawable;
	}

	public int getDrawable()
	{
		return drawable;
	}

	public List<LibraryTrack> getTracks()
	{
		return tracks;
	}
	
	public int getTracksCount()
	{
		if (tracks == null) return 0;
		return tracks.size();
	}
	
	public LibraryTrack getTrack(int position)
	{
		if (tracks == null) return null;
		if (position < 0 || position >= tracks.size()) return null;
		return tracks.get(position);
	}
	
	public void initTracks(List<? extends Item> items)
	{
		if (items == null) 
		{
			tracks = null;
			return;
		}
		
		List<LibraryTrack> tracksTmp = new ArrayList<LibraryTrack>();
		int ID = 1;
		for (Item i : items)
    	{
			LibraryTrack track = new LibraryTrack((Music) i, this.ID + ID);
			tracksTmp.add(track);
    		ID++;
    	}
		tracks = tracksTmp;
	}
		
	public String getInfos(Context context, SharedPreferences settings)
	{
		boolean showYear = settings.getBoolean(SettingsHelper.ALBUM_SHOW_YEAR, true);
		boolean showTrackCount = settings.getBoolean(SettingsHelper.ALBUM_SHOW_TRACK_COUNT, true);
		boolean showDuration = settings.getBoolean(SettingsHelper.ALBUM_SHOW_DURATION, true);
        
        String text = "";
		
		if (showYear)
		{
			if (getYear() > 0)
			{
				text += getYear();
				if (showTrackCount || showDuration) text += " - ";
			}
		}
						
		if (showTrackCount) 
		{
			String format = (getSongCount()==1) ? context.getString(R.string.library_album_fmt_singleTrack) : context.getString(R.string.library_album_fmt_multiTrack);
			text += String.format(format, getSongCount());
		}
		
		if (showDuration)
		{
			if (getDuration() != 0)
			{
				if (showTrackCount) text += ", ";
				text += Music.timeToString(getDuration());
			}
		}
		
		return text;
	}

	public LibraryAlbumAdapter getAdapter() 
	{
		return adapter;
	}

	public void setAdapter(LibraryAlbumAdapter adapter) 
	{
		this.adapter = adapter;
	}

	public Drawable getDrawableCover() 
	{
		return drawableCover;
	}

	public void setDrawableCover(Drawable drawableCover) 
	{
		this.drawableCover = drawableCover;
	}

	public AlbumExpandableListView getList() 
	{
		return list;
	}

	public void setList(AlbumExpandableListView list) 
	{
		this.list = list;
	}

	public boolean isExpanded() 
	{
		return isExpanded;
	}

	public void setExpanded(boolean isExpanded) 
	{
		this.isExpanded = isExpanded;
	}	
	
}
