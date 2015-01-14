package org.thunder.mpdcontrol.cover;

import org.thunder.mpdcontrol.mpd.AlbumInfo;

public interface CoverDownloadListener 
{

    public void onCoverDownloaded(CoverInfo cover);
    public void onCoverDownloadStarted(CoverInfo cover);
    public void onCoverNotFound(CoverInfo coverInfo);
    public void tagAlbumCover(AlbumInfo albumInfo);

}
