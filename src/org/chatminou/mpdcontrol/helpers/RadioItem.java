package org.chatminou.mpdcontrol.helpers;

import org.chatminou.mpdcontrol.mpd.Music;

public class RadioItem 
{
	public static String TAG = "::##[[stream]]##::";
	
	private String name = null;
	private String url = null;
    private String cover = null;
    
    public RadioItem(String name, String url, String cover)
    {
    	this.name = name;
    	this.url = url;
    	this.cover = cover;
    }
    
    public String getName()
    {
    	return name;
    }
    
    public String getUrl()
    {
    	return url;
    }
    
    public String getCover()
    {
    	return cover;
    }
    
    public Music parseSong(Music in)
    {
    	String title = "";
    	String artist = "";
    	String album = "";
    	
    	if (in.haveTitle()) 
    	{
    		title = in.getTitle();
    		String tab[] = title.split("-", 2);
        	if (tab.length == 2)
        	{
        		artist = tab[0].trim();
        		title = tab[1].trim();
        	}
        	//album = in.getName();
        	album = name;
    	}
    	else
    	{
    		//title = in.getName();
    		title = name;
    	}
    	
    	Music out = new Music();
    	out.setTitle(title);
	 	out.setArtist(artist);
	 	out.setAlbum(album);
    	return out;
    }
	
}
