package org.thunder.mpdcontrol.cover.provider;

import java.io.File;
import java.io.FileOutputStream;

import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.cover.ICoverRetriever;
import org.thunder.mpdcontrol.helpers.RadioItem;
import org.thunder.mpdcontrol.mpd.AlbumInfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

public class RadioCover implements ICoverRetriever 
{

    private Context context;
    private static final String FOLDER_SUFFIX = "/covers/";

    public RadioCover(MainApplication context) 
    {
        if (context == null) throw new RuntimeException("Context cannot be null");
        this.context = context;
    }

    public String getAbsoluteCoverFolderPath() 
    {
        final File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) return null;
        return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
    }

    public String getAbsolutePathForSong(String cover) 
    {
        final File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) return null;
        return getAbsoluteCoverFolderPath() + "radiostore." + cover + ".png";
    }

    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception 
    {
    	final String storageState = Environment.getExternalStorageState();
    	if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState) || Environment.MEDIA_MOUNTED.equals(storageState)) 
        {
    		if (albumInfo.getArtist().equals(RadioItem.TAG))
    		{
    			String cover = albumInfo.getAlbum();
    			String url = getAbsolutePathForSong(cover);
    			if (new File(url).exists())
    			{
                    return new String[] { url };
    			}
    			else
    			{
    				int id = context.getResources().getIdentifier("radiostore_" + cover, "drawable", context.getPackageName());
    				if (id != 0)
    				{
    					Bitmap bm = BitmapFactory.decodeResource(context.getResources(), id);
    				
    					File file = new File(url);
    					FileOutputStream outStream = new FileOutputStream(file);
    					bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
    					outStream.flush();
    					outStream.close();
    					
    					return new String[] { url };
    				}
    			}
    		}
    	}

        return null;
    }

    @Override
    public String getName() 
    {
        return "Local Radio Provider";
    }

    @Override
    public boolean isCoverLocal() 
    {
        return true;
    }

}
