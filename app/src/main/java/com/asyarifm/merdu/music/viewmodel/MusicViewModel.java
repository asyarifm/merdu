package com.asyarifm.merdu.music.viewmodel;

import android.app.Application;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.asyarifm.merdu.R;
import com.asyarifm.merdu.music.model.SongItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MusicViewModel extends AndroidViewModel {
    // init TAG Value for debugging purpose
    private String TAG = getClass().getSimpleName();

    // declare item related to API Request
    private RequestQueue requestQueue;
    private StringRequest stringRequest;
    private String url = "https://itunes.apple.com/search?entity=song&term=";           // init URL for API request

    // declare song list
    private ArrayList<SongItem> songItemList;

    // declare all mutable live data
    private MutableLiveData<ArrayList<SongItem>> _updateSongList;
    private MutableLiveData<Boolean> _isMusicPlaying;
    private MutableLiveData<Boolean> _isNewArtistSearch;
    private MutableLiveData<Boolean> _isLoadingDialogShowing;
    private MutableLiveData<String> _displayMessage;

    //declare media player
    private MediaPlayer mediaPlayer;

    public MusicViewModel(@NonNull Application application) {
        super(application);

        //init MutableLiveData
        _updateSongList = new MutableLiveData<>();
        _isNewArtistSearch = new MutableLiveData<>();
        _isMusicPlaying = new MutableLiveData<>();
        _isLoadingDialogShowing = new MutableLiveData<>();
        _displayMessage = new MutableLiveData<>();

        //init song list
        songItemList = new ArrayList<>();

        //init requestQueue
        requestQueue = Volley.newRequestQueue(getApplication());

        // initializing media player
        mediaPlayer = new MediaPlayer();
        // stream type for media player.
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // once media player finish playing the song
        // update the observer
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                _isMusicPlaying.postValue(false);
            }
        });
    }

    // fetching song data based on artist
    public void fetchSongData(String artist) {
        // combine url with artist name keyed in by user
        String fullUrl = "";
        try {
            // encode artist to URL safe string as per API instruction
            fullUrl = url + URLEncoder.encode(artist, "utf-8");
            Log.d(TAG, "full url: " + fullUrl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(TAG, "fail to do URL encoding, exception: " + e.getMessage());
            return;
        }

        // update observer that viewmodel start fetching new song data
        _isNewArtistSearch.postValue(true);
        //clear song list before fetching data
        songItemList.clear();

        //display loading dialog
        _isLoadingDialogShowing.postValue(true);

        // init string request
        stringRequest = new StringRequest(Request.Method.GET, fullUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //hide loading dialog
                _isLoadingDialogShowing.postValue(false);

                Log.d(TAG, "Response :" + response);
                try {
                    // convert json string to json object
                    JSONObject responseObj = new JSONObject(response);
                    // get result count
                    int resultCounts = responseObj.optInt("resultCount", 0);
                    // if result count greater than 0, put the data into song list
                    // else display message to user
                    if (resultCounts > 0) {
                        JSONArray songArray = responseObj.getJSONArray("results");
                        for (int i = 0; i < resultCounts; i++) {
                            String artistName = songArray.getJSONObject(i).optString("artistName", "");
                            if (artistName.equalsIgnoreCase(artist)) {
                                songItemList.add(SongItem.createFromJson(songArray.getJSONObject(i)));
                            }
                        }
                    } else {
                        //display message to user
                        _displayMessage.postValue(getApplication().getString(R.string.error_song_data_not_found));
                    }
                } catch (JSONException e) {
                    //display message to user
                    _displayMessage.postValue(getApplication().getString(R.string.error_fetching_song_data));

                    e.printStackTrace();
                    Log.e(TAG, "fail to fetch the data, exception: " + e.getMessage());
                }

                // if song list empty
                if (songItemList.size() <= 0) {
                    //display message to user
                    _displayMessage.postValue(getApplication().getString(R.string.error_song_data_not_found));
                }
                // update song list to observer
                _updateSongList.postValue(songItemList);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //hide loading dialog
                _isLoadingDialogShowing.postValue(false);
                //display message to user
                _displayMessage.postValue(getApplication().getString(R.string.error_networking_problem));

                Log.i(TAG,"Volley Error :" + error.toString());
            }
        });
        // add string request to request Queue
        requestQueue.add(stringRequest);
    }

    // prepare song to be played later
    public void prepareMusic(SongItem item) {
        // if item null or item previewurl is empty
        // no need to prepare anything
        if (item == null || item.previewUrl.isEmpty()) {
            Log.d(TAG, "no music to prepare");
            return;
        }

        // preparing media player
        try {
            // if media player is playing, stop it
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                // update observer
                _isMusicPlaying.postValue(false);
            }

            // reset the media player
            mediaPlayer.reset();

            // set song URL
            mediaPlayer.setDataSource(item.previewUrl);

            // prepare music
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "fail to prepare audio, exception: " + e.getMessage());
        }
    }

    // play or pause selected song
    public void playPauseMusic() {
        // check media player
        if (mediaPlayer == null) {
            Log.e(TAG, "media player is null");
            return;
        }

        // if media player currently playing, pause media player
        // otherwise play media player
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        } else{
            mediaPlayer.pause();
        }

        // update data to observer
        _isMusicPlaying.postValue(mediaPlayer.isPlaying());
    }

    // Livedata functions for observer
    public LiveData<ArrayList<SongItem>> updateSongList() {
        return _updateSongList;
    }
    public LiveData<Boolean> isMusicPlaying() {
        return _isMusicPlaying;
    }
    public LiveData<Boolean> isNewArtisSearch() {
        return _isNewArtistSearch;
    }
    public LiveData<Boolean> isLoadingDialogShowing() {
        return _isLoadingDialogShowing;
    }
    public LiveData<String> displayMessage() {
        return _displayMessage;
    }
}
