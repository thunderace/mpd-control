package org.thunder.mpdcontrol.models;

import static org.thunder.mpdcontrol.tools.StringUtils.getExtension;

import org.thunder.mpdcontrol.mpd.Music;

public class PlaylistStream extends AbstractPlaylistMusic 
{

    public PlaylistStream(Music m) 
    {
        super(m.getAlbum(), m.getArtist(), m.getAlbumArtist(), m.getFullpath(), m.getDisc(), m
                .getDate(), m.getTime(), m.getParentDirectory(), m.getTitle(), m.getTotalTracks(),
                m.getTrack(), m.getSongId(), m.getPos(), m.getName());
    }

    public String getPlayListMainLine() 
    {
        return getName().replace("." + getExtension(getName()), "");
    }

    public String getPlaylistSubLine() 
    {
        return getFullpath();
    }

}
