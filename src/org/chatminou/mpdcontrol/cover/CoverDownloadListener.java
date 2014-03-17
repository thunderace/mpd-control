package org.chatminou.mpdcontrol.cover;

import org.chatminou.mpdcontrol.mpd.AlbumInfo;

public interface CoverDownloadListener 
{

    public void onCoverDownloaded(CoverInfo cover);
    public void onCoverDownloadStarted(CoverInfo cover);
    public void onCoverNotFound(CoverInfo coverInfo);
    public void tagAlbumCover(AlbumInfo albumInfo);

}
