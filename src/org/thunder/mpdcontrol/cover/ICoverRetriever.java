package org.thunder.mpdcontrol.cover;

import org.thunder.mpdcontrol.mpd.AlbumInfo;

public interface ICoverRetriever 
{

    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception;
    public String getName();
    public boolean isCoverLocal();
    
}
