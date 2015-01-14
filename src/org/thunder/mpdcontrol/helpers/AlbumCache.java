package org.thunder.mpdcontrol.helpers;

import android.content.Context;
import android.util.Log;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.mpd.MPD;
import org.thunder.mpdcontrol.mpd.MPDCommand;
import org.thunder.mpdcontrol.mpd.MPDConnection;
import org.thunder.mpdcontrol.mpd.Music;
import org.thunder.mpdcontrol.mpd.exception.MPDServerException;
import org.thunder.mpdcontrol.tools.Tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AlbumCache
{
	public static final boolean DEBUG = false;
	
	private void d(String a) 
	{ 
		if (DEBUG) 
			Log.d(MainApplication.TAG, "AlbumCache: " + a); 
	}
	
    class AlbumDetails implements Serializable 
    {
        private static final long serialVersionUID = 2465675380232237273L;
        String path = null;
        // List<Long> times = null;
        long numtracks = 0;
        long totaltime = 0;
        long date = 0;

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException 
        {
            path = in.readUTF();
            // times = (List<Long>)in.readObject();
            numtracks = in.readLong();
            totaltime = in.readLong();
            date = in.readLong();
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException 
        {
            out.writeUTF(path);
            // out.writeObject(times);
            out.writeLong(numtracks);
            out.writeLong(totaltime);
            out.writeLong(date);
        }
    }

    static final boolean GZIP = false;

    public static AlbumCache getInstance(CachedMPD mpd) 
    {
        if (instance == null) instance = new AlbumCache(mpd);
        else instance.setMPD(mpd);
        return instance;
    }

    protected boolean enabled = true;
    protected String server;

    protected int port;
    protected File filesdir;
    protected CachedMPD mpd;

    protected MPDConnection mpdconnection;
    protected Date lastUpdate = null;

    // list of albumname, artist, albumartist including ""
    protected Set<List<String>> albumSet;

    // albums that have an albumartist get an empty artist:
    protected Set<List<String>> uniqueAlbumSet;

    protected Map<String, AlbumDetails> albumDetails; // "artist///album" -> details

    protected static AlbumCache instance = null;

    protected AlbumCache(CachedMPD _mpd) 
    {
        d("Starting ...");
        setMPD(_mpd);
    }

    public String albumCode(String artist, String album, boolean isAlbumArtist) 
    {
        return (artist != null ? artist : "") + "//" +
                (isAlbumArtist ? "AA" : "A") +
                "//" + (album != null ? album : "");
    }

    public String cacheInfo() 
    {
        return "AlbumCache: " +
                albumSet.size() + " album/artist combinations, " +
                uniqueAlbumSet.size() + " unique album/artist combinations, " +
                "Date: " + lastUpdate;
    }

    protected synchronized void deleteFile() 
    {
        File file = new File(filesdir, getFilename());
        d("Deleting " + file);
        if (file.exists())
            file.delete();
    }

    public Set<String> getAlbumArtists(String album, String artist) 
    {
        Set<String> aartists = new HashSet<String>();
        for (List<String> ai : albumSet) 
        {
            if (ai.get(0).equals(album) && ai.get(1).equals(artist)) 
            {
                aartists.add(ai.get(2));
            }
        }
        return aartists;
    }

    public AlbumDetails getAlbumDetails(String artist, String album, boolean isAlbumArtist) 
    {
        return albumDetails.get(albumCode(artist, album, isAlbumArtist));
    }

    public Set<String> getAlbums(String artist, boolean albumArtist) 
    {
        Set<String> albums = new HashSet<String>();
        for (List<String> ai : albumSet) 
        {
            if (albumArtist && ai.get(2).equals(artist) || !albumArtist && ai.get(1).equals(artist)) 
            {
                albums.add(ai.get(0));
            }
        }
        return albums;
    }

    public Set<List<String>> getAlbumSet() 
    {
        return albumSet;
    }

    public List<String> getArtistsByAlbum(String album, boolean albumArtist) 
    {
        Set<String> artists = new HashSet<String>();
        for (List<String> ai : albumSet) 
        {
            if (ai.get(0).equals(album)) 
            {
                if (albumArtist) 
                {
                    artists.add(ai.get(2));
                } 
                else 
                {
                    artists.add(ai.get(1));
                }
            }
        }
        List<String> result;
        if (artists != null && artists.size() > 0) 
        {
            result = new ArrayList<String>(artists);
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        }
        else 
        {
            result = new ArrayList<String>();
        }
        return result;
    }

    public String getDirByArtistAlbum(String artist, String album, boolean isAlbumArtist) 
    {
        String aa = albumCode(artist, album, isAlbumArtist);
        String result = albumDetails.get(aa).path;
        d("key " + aa + " - " + result);
        return result;
    }

    protected String getFilename() 
    {
        return server + "_" + port;
    }

    protected Set<String> getKeysByValue(Map<String, Set<String>> map, String val) 
    {
        Set<String> result = new HashSet<String>();
        Set<String> keys = map.keySet();
        for (String k : keys) 
        {
            Set<String> values = map.get(k);
            if (val == null || val == "" || values.contains(val)) 
            {
                result.add(k);
            }
        }
        return result;
    }

    public Set<List<String>> getUniqueAlbumSet() 
    {
        return uniqueAlbumSet;
    }

    protected synchronized boolean isUpToDate() 
    {
        try 
        {
            Date mpdlast = mpd.getStatistics().getDbUpdate();
            d("lastupdate " + lastUpdate + " mpd date " + mpdlast);
            return (null != lastUpdate && null != mpdlast && lastUpdate.after(mpdlast));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return false;
        }
    }

    protected synchronized boolean load() 
    {
        File file = new File(filesdir, getFilename() + (GZIP ? ".gz" : ""));
        if (!file.exists()) 
        {
            return false;
        }
        d("Loading " + file);
        ObjectInputStream restore = null;
        boolean loaded_ok = false;
        try 
        {
            if (GZIP) 
            {
                restore = new ObjectInputStream(new GZIPInputStream
                        (new FileInputStream(file)));
            } 
            else 
            {
                restore = new ObjectInputStream(new FileInputStream(file));
            }
            lastUpdate = (Date) restore.readObject();
            albumDetails = (HashMap<String, AlbumDetails>) restore.readObject();
            albumSet = (Set<List<String>>) restore.readObject();
            restore.close();
            makeUniqueAlbumSet();
            loaded_ok = true;
        } 
        catch (FileNotFoundException e) 
        {
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        if (loaded_ok)
        	d(cacheInfo());
        else
        	d("Error on load");
        
        return loaded_ok;
    }

    protected void makeUniqueAlbumSet() 
    {
        uniqueAlbumSet = new HashSet<List<String>>();
        for (List<String> ai : albumSet) 
        {
            if ("".equals(ai.get(2))) 
            { 
            	// no albumartist
                uniqueAlbumSet.add(Arrays.asList(ai.get(0), ai.get(1), ""));
            } 
            else 
            { 
            	// with albumartist set artist to ""
                uniqueAlbumSet.add(Arrays.asList(ai.get(0), "", ai.get(2)));
            }
        }
    }

    /*
     * reloads info from MPD if it is not up to date
     */
    public synchronized boolean refresh() 
    {
        return refresh(false);
    }

    /*
     * reloads info from MPD if it is not up to date or if forced
     */
    public synchronized boolean refresh(boolean force) 
    {
        if (!enabled) return false;
        if (!updateConnection()) return false;

        if (!force && isUpToDate()) 
        {
        	d("Cache is up to date");
            return true;
        }
        
        d("Cache is NOT up to date. fetching ...");
        lastUpdate = Calendar.getInstance().getTime();

        Context context = MPD.getApplicationContext();
        Tools.notifyUser(context.getResources().getString
                (R.string.album_cache_updating_note), context);
        
        Date oldUpdate = lastUpdate;
        albumDetails = new HashMap<String, AlbumDetails>();
        albumSet = new HashSet<List<String>>();

        List<Music> allmusic;
        try 
        {
            
        	allmusic = Music.getMusicFromList(
                    mpdconnection.sendCommand(MPDCommand.MPD_CMD_LISTALLINFO), false);
        	d("allmusic " + allmusic.size());
        } 
        catch (MPDServerException e) 
        {
            enabled = false;
            lastUpdate = null;
            e.printStackTrace();
            updateConnection();
            d("disabled AlbumCache");
            Tools.notifyUser(
                    "Error with the 'listallinfo' command. Probably you have to adjust your server's 'max_output_buffer_size'",
                    context);
            return false;
        }

        try 
        {
            for (Music m : allmusic) 
            {
                String albumartist = m.getAlbumArtist();
                String artist = m.getArtist();
                String album = m.getAlbum();
                if (album == null) 
                {
                    album = "";
                }
                List<String> albuminfo = Arrays.asList
                        (album, artist == null ? "" : artist,
                                albumartist == null ? "" : albumartist);
                albumSet.add(albuminfo);

                boolean isAA = albumartist != null && !("".equals(albumartist));
                String thisalbum =
                        albumCode(isAA ? albumartist : artist, album, isAA);
                AlbumDetails details;
                if (albumDetails.containsKey(thisalbum)) 
                {
                    details = albumDetails.get(thisalbum);
                } 
                else 
                {
                    details = new AlbumDetails();
                    albumDetails.put(thisalbum, details);
                }
                if (details.path == null) 
                {
                    details.path = m.getPath();
                }
                // if (details.times == null)
                // details.times = new ArrayList<Long>();
                // details.times.add((Long)m.getTime());
                details.numtracks += 1;
                details.totaltime += m.getTime();
                if (details.date == 0)
                    details.date = m.getDate();
            }
            d("albumDetails: " + albumDetails.size());
            d("albumSet: " + albumSet.size());
            makeUniqueAlbumSet();
            d("uniqueAlbumSet: " + uniqueAlbumSet.size());
            if (!save()) {
                lastUpdate = oldUpdate;
                return false;
            }
        } 
        catch (Exception e) 
        {
            Tools.notifyUser("Error updating Album Cache", context);
            lastUpdate = oldUpdate;
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected synchronized boolean save() 
    {
        File file = new File(filesdir, getFilename() + (GZIP ? ".gz" : ""));
        d("Saving to " + file);
        File backupfile = new File(file.getAbsolutePath() + ".bak");
        if (file.exists()) 
        {
            if (backupfile.exists())
                backupfile.delete();
            file.renameTo(backupfile);
        }
        ObjectOutputStream save = null;
        boolean error = false;
        try {
            if (GZIP) 
            {
                save = new ObjectOutputStream(new GZIPOutputStream
                        (new FileOutputStream(file)));
            } 
            else 
            {
                save = new ObjectOutputStream(new BufferedOutputStream
                        (new FileOutputStream(file)));
            }
            save.writeObject(lastUpdate);
            save.writeObject(albumDetails);
            save.writeObject(albumSet);
            save.close();
            d("saved to " + file);
        } 
        catch (Exception e) 
        {
            error = true;
            e.printStackTrace();
        }
        if (error) 
        {
            file.delete();
            backupfile.renameTo(file);
        }
        return !error;
    }

    protected void setMPD(CachedMPD _mpd) 
    {
        enabled = true;
        try 
        {
        	d("set MPD");
            this.mpd = _mpd;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        updateConnection();
    }

    protected synchronized boolean updateConnection() 
    {
        // get server/port from mpd
        if (!enabled) 
        {
        	d("is disabled");
            return false;
        }
        if (mpd == null) 
        {
        	d("no MPD! ");
            return false;
        }
        mpdconnection = mpd.getMpdConnection();
        if (mpdconnection == null) 
        {
        	d("no MPDConnection! ");
            return false;
        }
        if (this.server == null) 
        {
            this.server = mpdconnection.getHostAddress().getHostName();
            this.port = mpdconnection.getHostPort();
            this.filesdir = MPD.getApplicationContext().getCacheDir();
            d("server " + server + " port " + port + " dir " + filesdir);
            if (!load()) 
            {
                refresh(true);
            }
        }
        return true;
    }

}
