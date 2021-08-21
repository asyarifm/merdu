package com.asyarifm.merdu.music.viewmodel;

import android.app.Application;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

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
    private String url = "https://itunes.apple.com/search?entity=song&limit=200&term=";           // init URL for API request

    // declare song list
    private ArrayList<SongItem> songItemList;

    // declare all mutable live data
    private MutableLiveData<ArrayList<SongItem>> _updateSongList;
    private MutableLiveData<Boolean> _isMusicPlaying;
    private MutableLiveData<Boolean> _isLoadingDialogShowing;
    private MutableLiveData<Integer> _getMusicPlayerVisibility;
    private MutableLiveData<String> _getSelectedSongTitle;
    private MutableLiveData<String> _getSelectedArtistName;
    private MutableLiveData<String> _displayMessage;
    private MutableLiveData<Integer> _updateSelectedPosition;
    private MutableLiveData<Integer> _updatePlayingPosition;

    //declare media player
    private MediaPlayer mediaPlayer;
    //init prepared song
    private SongItem preparedSong = null;

    // init selected position and playing position
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int playingPosition = RecyclerView.NO_POSITION;

    public MusicViewModel(@NonNull Application application) {
        super(application);

        //init MutableLiveData
        _updateSongList = new MutableLiveData<>();
        _isMusicPlaying = new MutableLiveData<>();
        _isLoadingDialogShowing = new MutableLiveData<>();
        _displayMessage = new MutableLiveData<>();
        _getMusicPlayerVisibility = new MutableLiveData<>();
        _getSelectedSongTitle = new MutableLiveData<>();
        _getSelectedArtistName = new MutableLiveData<>();
        _updateSelectedPosition = new MutableLiveData<>();
        _updatePlayingPosition = new MutableLiveData<>();

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
                setPlayingPosition(RecyclerView.NO_POSITION);
            }
        });
    }

    // fetching song data based on artist
    public void fetchSongData(String artist) {
        // if search empty, update with exsiting song list
        // and also existing selected position and playing position
        if (artist.isEmpty()) {
            _updateSongList.postValue(songItemList);
            setSelectedPosition(this.selectedPosition);
            setPlayingPosition(this.playingPosition);
            return;
        }
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
                setSelectedPosition(RecyclerView.NO_POSITION);
                setPlayingPosition(RecyclerView.NO_POSITION);
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
    private void prepareMusic(SongItem item) {
        // if item null or item previewurl is empty
        // no need to prepare anything
        if (item == null || item.previewUrl.isEmpty()) {
            Log.d(TAG, "no music to prepare");
            return;
        }

        if (preparedSong != null) {
            if (preparedSong.previewUrl.equalsIgnoreCase(item.previewUrl)) {
                Log.d(TAG, "same song have been prepared, do nothing");
                return;
            }
        }

        // preparing media player
        try {
            // if media player is playing, stop it
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                // update observer
                _isMusicPlaying.postValue(false);
                setPlayingPosition(RecyclerView.NO_POSITION);
            }

            // reset the media player
            mediaPlayer.reset();

            // set song URL
            mediaPlayer.setDataSource(item.previewUrl);

            // prepare music
            mediaPlayer.prepare();

            // update prepared song
            preparedSong = item;
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
        if (mediaPlayer.isPlaying()) {
            setPlayingPosition(this.selectedPosition);
        } else {
            setPlayingPosition(RecyclerView.NO_POSITION);
        }
    }

    public void onSelectedSongItem(int selectedPosition) {
        SongItem item = null;
        if (selectedPosition >= 0 && selectedPosition < songItemList.size()) {
            item = songItemList.get(selectedPosition);
        }

        if (item != null) {
            // prepare music based on selected song
            prepareMusic(item);
            setSelectedPosition(selectedPosition);

            _getMusicPlayerVisibility.postValue(View.VISIBLE);
            _getSelectedSongTitle.postValue(item.songTitle);
            _getSelectedArtistName.postValue(item.artistName);
        } else {
            setSelectedPosition(RecyclerView.NO_POSITION);

            if (mediaPlayer.isPlaying()) {
                _getMusicPlayerVisibility.postValue(View.VISIBLE);
            } else {
                _getMusicPlayerVisibility.postValue(View.GONE);
            }
        }
    }

    // ondestroy
    public void onDestroy() {
        // release media player
        mediaPlayer.release();
    }

    // set selected song item position
    private void setSelectedPosition(int position) {
        selectedPosition = position;
        _updateSelectedPosition.postValue(selectedPosition);
    }

    // set playing song item position
    private void setPlayingPosition(int position) {
        playingPosition = position;
        _updatePlayingPosition.postValue(playingPosition);
    }

    // Livedata functions for observer
    public LiveData<ArrayList<SongItem>> updateSongList() {
        return _updateSongList;
    }
    public LiveData<Boolean> isMusicPlaying() {
        return _isMusicPlaying;
    }
    public LiveData<Boolean> isLoadingDialogShowing() {
        return _isLoadingDialogShowing;
    }
    public LiveData<String> displayMessage() {
        return _displayMessage;
    }
    public LiveData<Integer> getMusicPlayerVisibility() {
        return _getMusicPlayerVisibility;
    }
    public LiveData<String> getSelectedSongTitle() {
        return _getSelectedSongTitle;
    }
    public LiveData<String> getSelectedArtistName() {
        return _getSelectedArtistName;
    }
    public LiveData<Integer> updateSelectedPosition() {
        return _updateSelectedPosition;
    }
    public LiveData<Integer> updatePlayingPosition() {
        return _updatePlayingPosition;
    }
}
