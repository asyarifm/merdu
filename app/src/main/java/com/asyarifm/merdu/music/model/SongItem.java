package com.asyarifm.merdu.music.model;

import android.util.Log;

import org.json.JSONObject;

public class SongItem {

    private static String TAG = SongItem.class.getSimpleName();

    public int artistId;
    public String artistName;
    public String artworkUrl60;
    public String artworkUrl100;
    public String albumName;
    public String songTitle;
    public String previewUrl;
//    public String genreName;
//    public String releaseDate;

    // create SongItem object from received json object
    public static SongItem createFromJson(JSONObject object) {
        SongItem song = new SongItem();
        //assign value accordingly
        song.artistId = object.optInt("artistId", 0);
        song.artistName = object.optString("artistName", "");
        song.songTitle = object.optString("trackName", "");
        song.artworkUrl60 = object.optString("artworkUrl60", "");
        song.artworkUrl100 = object.optString("artworkUrl100", "");
        song.albumName = object.optString("collectionName", "");
        song.previewUrl = object.optString("previewUrl", "");
//        song.genreName = object.optString("primaryGenreName", "");
//        song.releaseDate = object.optString("releaseDate", "");

        Log.d(TAG, "created song item from: " + object.toString());
        return song;
    }

}
