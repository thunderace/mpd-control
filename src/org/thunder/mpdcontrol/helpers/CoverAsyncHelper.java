package org.thunder.mpdcontrol.helpers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.cover.CoverDownloadListener;
import org.thunder.mpdcontrol.cover.CoverInfo;
import org.thunder.mpdcontrol.cover.CoverManager;
import org.thunder.mpdcontrol.mpd.AlbumInfo;
import org.thunder.mpdcontrol.mpd.MPD;
import org.thunder.mpdcontrol.tools.Tools;

import java.util.Collection;
import java.util.LinkedList;

public class CoverAsyncHelper extends Handler implements CoverDownloadListener 
{
    public static final int EVENT_COVER_DOWNLOADED = 1;
    public static final int EVENT_COVER_NOT_FOUND = 2;
    public static final int EVENT_COVER_DOWNLOAD_STARTED = 3;
    public static final int MAX_SIZE = 0;

    private static final Message COVER_NOT_FOUND_MESSAGE;

    static 
    {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVER_NOT_FOUND;
    }

    private MainApplication app = null;
    private SharedPreferences settings = null;

    private int coverMaxSize = MAX_SIZE;
    private int cachedCoverMaxSize = MAX_SIZE;

    private Collection<CoverDownloadListener> coverDownloadListener;

    public CoverAsyncHelper(MainApplication app, SharedPreferences settings) 
    {
        this.app = app;
        this.settings = settings;
        coverDownloadListener = new LinkedList<CoverDownloadListener>();
    }

    public void addCoverDownloadListener(CoverDownloadListener listener) 
    {
        coverDownloadListener.add(listener);
    }

    private void displayCoverRetrieverName(CoverInfo coverInfo) 
    {
        try 
        {
            if (!coverInfo.getCoverRetriever().isCoverLocal()) 
            {
                String message = "\"" + coverInfo.getAlbum() + "\" cover found with " + coverInfo.getCoverRetriever().getName();
                Tools.notifyUser(message, MPD.getApplicationContext());
            }
        } 
        catch (Exception e) 
        {
        }
    }

    public void downloadCover(AlbumInfo albumInfo) 
    {
        downloadCover(albumInfo, false);
    }

    public void downloadCover(AlbumInfo albumInfo, boolean priority) 
    {
        final CoverInfo info = new CoverInfo(albumInfo);
        info.setCoverMaxSize(coverMaxSize);
        info.setCachedCoverMaxSize(cachedCoverMaxSize);
        info.setPriority(priority);
        info.setListener(this);
        tagListenerCovers(albumInfo);

        if (!albumInfo.isValid()) 
        {
            COVER_NOT_FOUND_MESSAGE.obj = info;
            handleMessage(COVER_NOT_FOUND_MESSAGE);
        } 
        else 
        {
            CoverManager.getInstance(app, settings).addCoverRequest(info);
        }
    }

    public void handleMessage(Message msg) 
    {
        switch (msg.what) 
        {
            case EVENT_COVER_DOWNLOADED:
                CoverInfo coverInfo = (CoverInfo) msg.obj;
                if (coverInfo.getCachedCoverMaxSize() < cachedCoverMaxSize || coverInfo.getCoverMaxSize() < coverMaxSize) 
                {
                	// We've got the wrong size, get it again from the cache
                	downloadCover(coverInfo);
                	break;
                }                
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverDownloaded(coverInfo);
                if (CoverManager.DEBUG)
                    displayCoverRetrieverName(coverInfo);
                break;

            case EVENT_COVER_NOT_FOUND:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverNotFound((CoverInfo) msg.obj);
                break;
                
            case EVENT_COVER_DOWNLOAD_STARTED:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverDownloadStarted((CoverInfo) msg.obj);
                break;
                
            default:
                break;
        }
    }

    @Override
    public void onCoverDownloaded(CoverInfo cover) 
    {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_DOWNLOADED, cover).sendToTarget();
    }

    @Override
    public void onCoverDownloadStarted(CoverInfo cover) 
    {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_DOWNLOAD_STARTED, cover).sendToTarget();
    }

    @Override
    public void onCoverNotFound(CoverInfo cover) 
    {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_NOT_FOUND, cover).sendToTarget();
    }

    public void removeCoverDownloadListener(CoverDownloadListener listener) 
    {
        coverDownloadListener.remove(listener);
    }
    
    public void freeCoverDownloadListeners() 
    {
    	for (CoverDownloadListener listener : coverDownloadListener) 
        {
            listener = null;
        }
    	coverDownloadListener.clear();
    }
    
    public int coverDownloadListenerCount()
    {
    	return coverDownloadListener.size();
    }

    /*
     * If you want cached images to be read as a different size than the
     * downloaded ones. If this equals MAX_SIZE, it will use the coverMaxSize
     * (if not also MAX_SIZE) Example : useful for NowPlayingSmallFragment,
     * where it's useless to read a big image, but since downloading one will
     * fill the cache, download it at a bigger size.
     */
    public void setCachedCoverMaxSize(int size) 
    {
        if (size < 0) size = MAX_SIZE;
        cachedCoverMaxSize = size;
    }

    public void setCoverMaxSize(int size) 
    {
        if (size < 0) size = MAX_SIZE;
        coverMaxSize = size;
    }

    public void setCoverMaxSizeFromScreen(Activity activity) 
    {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setCoverMaxSize(Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    public void setCoverRetrieversFromPreferences() 
    {
        CoverManager.getInstance(app, settings).setCoverRetrieversFromPreferences();
    }

    @Override
    public void tagAlbumCover(AlbumInfo albumInfo) 
    {
    }

    private void tagListenerCovers(AlbumInfo albumInfo) 
    {
        for (CoverDownloadListener listener : coverDownloadListener) 
        {
            listener.tagAlbumCover(albumInfo);
        }
    }
    
}
