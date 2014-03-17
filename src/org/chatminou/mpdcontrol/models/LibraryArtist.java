package org.chatminou.mpdcontrol.models;

import java.util.ArrayList;
import java.util.List;

import org.chatminou.mpdcontrol.mpd.Album;
import org.chatminou.mpdcontrol.mpd.Artist;
import org.chatminou.mpdcontrol.mpd.Item;
import org.chatminou.mpdcontrol.R;

public class LibraryArtist extends Artist 
{

	private int ID = -1;
	private int drawable = -1;
	private List<LibraryAlbum> albums = null;
	
	public LibraryArtist(Artist a) 
	{
		super(a);
		this.ID = -1;
		this.drawable = -1;
		this.albums = null;
	}
	
	public LibraryArtist(Artist a, int ID) 
	{
		super(a);
		this.ID = ID;
		this.drawable = -1;
		this.albums = null;
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

	public List<LibraryAlbum> getAlbums()
	{
		return albums;
	}
	
	public int getAlbumsCount()
	{
		if (albums == null) return 0;
		return albums.size();
	}
	
	public LibraryAlbum getAlbum(int position)
	{
		if (albums == null) return null;
		if (position < 0 || position >= albums.size()) return null;
		return albums.get(position);
	}
	
	public void initAlbums(List<? extends Item> items)
	{
		if (items == null) 
		{
			albums = null;
			return;
		}

		List<LibraryAlbum> albumsTmp = new ArrayList<LibraryAlbum>();
		int ID = 1;
		for (Item i : items)
    	{
			LibraryAlbum album = new LibraryAlbum((Album) i, this.ID + 1000*ID);
			album.setDrawable(this.drawable);
    		albumsTmp.add(album);
    		ID++;
    	}
		albums = albumsTmp;
	}

}
