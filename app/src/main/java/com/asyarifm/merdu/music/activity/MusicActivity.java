package com.asyarifm.merdu.music.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asyarifm.merdu.R;
import com.asyarifm.merdu.music.adapter.SongListAdapter;
import com.asyarifm.merdu.music.model.SongItem;
import com.asyarifm.merdu.music.utils.LoadingDialog;
import com.asyarifm.merdu.music.viewmodel.MusicViewModel;

import java.util.ArrayList;

public class MusicActivity extends AppCompatActivity implements SongListAdapter.OnClickListener {
    // init TAG Value for debugging purpose
    private String TAG = getClass().getSimpleName();

    // declare viewmodel
    private MusicViewModel viewModel;

    // declare loading dialog
    private LoadingDialog loadingDialog;

    // declare recycleview and its adapter
    private RecyclerView musicRecycleView;
    private SongListAdapter songListAdapter;

    // declare Music player layout and its button
    private View layoutMusicPlayer;
    private ImageButton buttonPlayPause;
    //declare selected song title and artist name tv
    private TextView selectedSongTitleTextView, selectedArtistNameTextView;

    // declare emptylist textview
    private TextView emptyListTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the view into activity_music layout
        setContentView(R.layout.activity_music);

        // init viewModel
        viewModel = new ViewModelProvider(this).get(MusicViewModel.class);

        // init loading dialog
        loadingDialog = new LoadingDialog(this);

        // init empty list textview
        // this empty list will only show if song list is empty
        emptyListTextView = findViewById(R.id.tv_empty_list);

        // init song list adapter
        songListAdapter = new SongListAdapter(this);
        songListAdapter.setOnClickListener(this);

        // set up music recycle view
        musicRecycleView = findViewById(R.id.rv_music);
        musicRecycleView.setAdapter(songListAdapter);
        musicRecycleView.setLayoutManager(new LinearLayoutManager(this));
        musicRecycleView.setHasFixedSize(true);

        // add divider line to music recycleview
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(musicRecycleView.getContext(), DividerItemDecoration.VERTICAL);
        musicRecycleView.addItemDecoration(dividerItemDecoration);

        // init layout music player
        layoutMusicPlayer = findViewById(R.id.layout_music_player);

        // init music player buttons
        buttonPlayPause = findViewById(R.id.button_play_pause);
        // set music player onclick listener
        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // play / pause the selected song
               viewModel.playPauseMusic();
            }
        });

        // init selected song title and artist name;
        selectedArtistNameTextView = findViewById(R.id.tv_selected_artist_name);
        selectedSongTitleTextView = findViewById(R.id.tv_selected_song_title);

        // Live data observer from viewmodel
        // display / hide loading dialog based on LiveData Value
        viewModel.isLoadingDialogShowing().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    loadingDialog.startLoading();
                } else {
                    loadingDialog.stopLoading();
                }
            }
        });
        // display a message sent by Live Data
        viewModel.displayMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                Toast.makeText(MusicActivity.this, s, Toast.LENGTH_LONG).show();
            }
        });
        // update song list from Live Data
        viewModel.updateSongList().observe(this, new Observer<ArrayList<SongItem>>() {
            @Override
            public void onChanged(ArrayList<SongItem> songItems) {
                Log.d(TAG, "songItems size: " + songItems.size());
                // update song list adapter
                songListAdapter.setItemList(songItems);
                if (songItems.size() > 0) {
                    musicRecycleView.setVisibility(View.VISIBLE);
                    emptyListTextView.setVisibility(View.GONE);
                } else {
                    musicRecycleView.setVisibility(View.GONE);
                    emptyListTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        // change play / pause button based of LiveData value
        viewModel.isMusicPlaying().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    songListAdapter.setPlayingPosition(songListAdapter.getSelectedPosition());
                    buttonPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause_indicator));
                } else {
                    songListAdapter.setPlayingPosition(RecyclerView.NO_POSITION);
                    buttonPlayPause.setImageDrawable(getDrawable(R.drawable.ic_play_indicator));
                }
            }
        });
        // start a new artist search once get notified by Livedata
        viewModel.isNewArtisSearch().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    songListAdapter.setPlayingPosition(RecyclerView.NO_POSITION);
                    songListAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
                }
            }
        });
    }

    //search menu for search song based on artist name
    public boolean createSearchMenu(Menu menu) {
        // inflate search menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search_song_by_artist, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        // set up search view
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint_artist_name));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // start fetching song data once user submit the query
                viewModel.fetchSongData(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    // song item on click listener
    @Override
    public void onClick(int position) {
        // update selected item position
        songListAdapter.setSelectedPosition(position);

        // prepare music based on selected song
        viewModel.prepareMusic(songListAdapter.getSelectedItem());

        // if selected position is valid, display layout music player
        if (songListAdapter.getSelectedPosition() != RecyclerView.NO_POSITION) {
            layoutMusicPlayer.setVisibility(View.VISIBLE);
            // set selected song title and artist name text view
            // if selected item song is not null
            SongItem selectedItem = songListAdapter.getSelectedItem();
            if (selectedItem != null) {
                selectedSongTitleTextView.setText(selectedItem.songTitle);
                selectedArtistNameTextView.setText(selectedItem.artistName);
            }
        } else {
            layoutMusicPlayer.setVisibility(View.GONE);
        }
    }

    // on create options menu listener
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // create search menu
        createSearchMenu(menu);
        return true;
    }
}
