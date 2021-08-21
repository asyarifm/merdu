package com.asyarifm.merdu.music.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asyarifm.merdu.R;
import com.asyarifm.merdu.music.model.SongItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ItemHolder> {
    private String TAG = SongListAdapter.class.getSimpleName();

    // declaration
    private Context ctx;
    private ArrayList<SongItem> itemList;
    private OnClickListener listener;

    // init selected position and playing position
    // both set to -1
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int playingPosition = RecyclerView.NO_POSITION;

    // listener for song item click
    public interface OnClickListener {
        void onClick(int position);
    }

    // constructor with context as parameter
    public SongListAdapter(Context ctx) {
        // init ctx and itemlist
        this.ctx = ctx;
        this.itemList = new ArrayList<>();
    }

    // set onclick listener
    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    //update item list and notify the changes
    public void setItemList(ArrayList<SongItem> itemList) {
        this.itemList = itemList;
        notifyDataSetChanged();
    }

    // get selected item
    // return song item if position valid (not -1);
    // else return null
    public SongItem getSelectedItem() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            return itemList.get(selectedPosition);
        } else {
            return null;
        }
    }

    // set selected song item position
    public void setSelectedPosition(int position) {
        // if position is not valid
        // update the selected position
        if (position == RecyclerView.NO_POSITION) {
            selectedPosition = position;
            return;
        }

        if (position == selectedPosition) {
            // if position equal to current selectedposition
            // notify the item at position on changes
            selectedPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(position);
        } else {
            // else notify previous selected position and also the new selected position
            int prevSelectedPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(prevSelectedPosition);
            notifyItemChanged(selectedPosition);
        }
    }

    // get selected position
    public int getSelectedPosition() {
        return selectedPosition;
    }

    //
    public void setPlayingPosition(int position) {
        Log.d(TAG, "setPlayingPosition: " + position);
        // if position is not valid
        // update the palying position
        if (position == RecyclerView.NO_POSITION) {
            playingPosition = RecyclerView.NO_POSITION;
            notifyDataSetChanged();
            return;
        }

        if (playingPosition != RecyclerView.NO_POSITION) {
            int prevPlayingPositiong = playingPosition;
            playingPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(prevPlayingPositiong);
        }

        if (position != RecyclerView.NO_POSITION) {
            playingPosition = position;
            notifyItemChanged(playingPosition);
        }
    }

    @Override
    public SongListAdapter.ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // inflate music item layout
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_music_item, viewGroup, false);
        return new SongListAdapter.ItemHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SongListAdapter.ItemHolder itemHolder, int position) {
        // get each object based on position
        SongItem currentItem = itemList.get(position);

        // if album art 60x60 empty try to get 100x100
        String albumArtUrl = currentItem.artworkUrl60;
        if (albumArtUrl.isEmpty()) {
            albumArtUrl = currentItem.artworkUrl100;
        }

        // if album art url not empty, create a new thread to load image from url
        if (!albumArtUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(albumArtUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemHolder.albumArt);
        }

        //put text into specific textview
        itemHolder.albumName.setText(currentItem.albumName);
        itemHolder.artistName.setText(currentItem.artistName);
        itemHolder.songTitle.setText(currentItem.songTitle);

        if (selectedPosition == position) {
            itemHolder.itemView.setBackgroundColor(ctx.getColor(R.color.black_primary_variant));
        } else {
            itemHolder.itemView.setBackgroundColor(ctx.getColor(R.color.black_primary));
        }

        if (playingPosition == position) {
            itemHolder.playingIndicator.setVisibility(View.VISIBLE);
        } else {
            itemHolder.playingIndicator.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {
        public ImageView albumArt;
        public ImageView playingIndicator;
        public TextView songTitle;
        public TextView artistName;
        public TextView albumName;

        public ItemHolder(View itemView, SongListAdapter.OnClickListener listener) {
            super(itemView);
            // link ui item
            albumArt = itemView.findViewById(R.id.iv_album_art);
            playingIndicator = itemView.findViewById(R.id.iv_play_indicator);
            songTitle = itemView.findViewById(R.id.tv_song_title);
            artistName = itemView.findViewById(R.id.tv_artist_name);
            albumName = itemView.findViewById(R.id.tv_album_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onClick(getAdapterPosition());
                    }
                }
            });
        }
    }
}
