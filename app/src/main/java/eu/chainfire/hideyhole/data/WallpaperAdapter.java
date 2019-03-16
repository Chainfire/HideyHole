/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package eu.chainfire.hideyhole.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.hideyhole.R;
import eu.chainfire.hideyhole.api.WallpaperResponse;

public class WallpaperAdapter extends PagedListAdapter<WallpaperResponse.Wallpaper, WallpaperAdapter.WallpaperViewHolder> {
    public interface Listener {
        void onClick(WallpaperResponse.Wallpaper wallpaper, ImageView imageView);
    }

    private Listener listener;

    public WallpaperAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new WallpaperViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_wallpapers, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static DiffUtil.ItemCallback<WallpaperResponse.Wallpaper> DIFF_CALLBACK = new DiffUtil.ItemCallback<WallpaperResponse.Wallpaper>() {
        @Override
        public boolean areItemsTheSame(WallpaperResponse.Wallpaper oldItem, WallpaperResponse.Wallpaper newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(WallpaperResponse.Wallpaper oldItem, WallpaperResponse.Wallpaper newItem) {
            return oldItem.equals(newItem);
        }
    };

    class WallpaperViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        WallpaperResponse.Wallpaper wallpaper;

        private View.OnClickListener onImageViewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(wallpaper, imageView);
                }
            }
        };

        public WallpaperViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            imageView.setOnClickListener(onImageViewClickListener);
        }

        public void bind(WallpaperResponse.Wallpaper wallpaper) {
            this.wallpaper = wallpaper;
            if (wallpaper != null) {
                Glide.with(imageView.getContext())
                        .load(wallpaper.thumbnail.url)
                        .into(imageView);
            }
        }
    }
}