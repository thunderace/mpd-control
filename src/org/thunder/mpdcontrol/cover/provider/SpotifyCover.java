package org.thunder.mpdcontrol.cover.provider;

import static android.text.TextUtils.isEmpty;

import org.json.JSONArray;
import org.json.JSONObject;
import org.thunder.mpdcontrol.mpd.AlbumInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetch cover from Spotify
 */
public class SpotifyCover extends AbstractWebCover {

    private List<String> extractAlbumIds(String response) {
        JSONObject jsonRoot;
        JSONArray jsonAlbums;
        JSONObject jsonAlbum;
        String albumId;
        List<String> albumIds = new ArrayList<String>();

        try {
            jsonRoot = new JSONObject(response);
            jsonAlbums = jsonRoot.optJSONArray("albums");
            if (jsonAlbums != null) {
                for (int a = 0; a < jsonAlbums.length(); a++) {
                    jsonAlbum = jsonAlbums.optJSONObject(a);
                    if (jsonAlbum != null) {
                        albumId = jsonAlbum.optString("href");
                        if (albumId != null && albumId.length() > 0) {
                            albumId = albumId.replace("spotify:album:", "");
                            albumIds.add(albumId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return albumIds;
    }

    private String extractImageUrl(String response) {

        JSONObject jsonAlbum;
        String imageUrl;

        try {
            jsonAlbum = new JSONObject(response);
            imageUrl = jsonAlbum.optString("thumbnail_url");
            return imageUrl;
        } catch (Exception e)

        {
            e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return null;
    }

    @Override
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {

        String albumResponse;
        List<String> albumIds;
        String coverResponse;
        String coverUrl;

        try {
            albumResponse = executeGetRequest("http://ws.spotify.com/search/1/album.json?q="
                    + albumInfo.getArtist() + " " + albumInfo.getAlbum());
            albumIds = extractAlbumIds(albumResponse);
            for (String albumId : albumIds) {
                coverResponse = executeGetRequest("https://embed.spotify.com/oembed/?url=http://open.spotify.com/album/"
                        + albumId);
                coverUrl = extractImageUrl(coverResponse);
                if (!isEmpty(coverUrl)) {
                    coverUrl = coverUrl.replace("/cover/", "/640/");
                    return new String[] {
                            coverUrl
                    };
                }
            }

        } catch (Exception e) {
            e(SpotifyCover.class.toString(), "Failed to get cover URL from Spotify");
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "SPOTIFY";
    }
}
