package org.chatminou.mpdcontrol.cover;

import org.chatminou.mpdcontrol.mpd.AlbumInfo;

public interface ICoverRetriever 
{

    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception;
    public String getName();
    public boolean isCoverLocal();
    
}
