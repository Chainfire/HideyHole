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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.hideyhole.R;
import eu.chainfire.hideyhole.api.WallpaperResponse;

public class WallpaperAdapter extends PagedListAdapter<WallpaperResponse.Wallpaper, WallpaperAdapter.WallpaperViewHolder> {
    private static final class Predownloader {
        // the latency on Google Cloud Storage is significantly higher than the latency on our
        // own server was, so we need to add some preloading. There's a Glide library for that
        // too, though it focuses more or preloading than pre_down_loading.

        private static final int PREDOWNLOAD_COUNT = 12;
        private static final int PREDOWNLOAD_THREADS = 4;

        private static Predownloader instance;
        public static Predownloader getInstance(Context context) {
            if (instance == null) instance = new Predownloader(context);
            return instance;
        }

        private final Context applicationContext;
        private final ArrayList<String> queued = new ArrayList<>();
        private final ArrayList<String> inflight = new ArrayList<>();
        private final ArrayList<String> completed = new ArrayList<>(); // keep short history as the same surrounding urls will be re-requested multiple times during scroll
        private final Thread[] threads = new Thread[PREDOWNLOAD_THREADS];

        private Predownloader(Context context) {
            this.applicationContext = context.getApplicationContext();
        }

        public void predownload(WallpaperResponse.Wallpaper wallpaper) {
            String url = wallpaper.thumbnail.url;
            synchronized (applicationContext) {
                if (queued.contains(url) || inflight.contains(url) || completed.contains(url)) {
                    return;
                }
                queued.add(url);
                while (queued.size() > PREDOWNLOAD_COUNT) {
                    queued.remove(0);
                }
                for (int i = 0; i < PREDOWNLOAD_THREADS; i++) {
                    if (threads[i] == null) {
                        threads[i] = new PredownloadThread(i);
                        threads[i].start();
                        break;
                    }
                }
            }
        }

        private class PredownloadThread extends Thread {
            private final int index;

            public PredownloadThread(int index) {
                this.index = index;
            }

            private String next() {
                synchronized (applicationContext) {
                    if (queued.size() == 0) {
                        threads[index] = null;
                        return null;
                    }
                    String url = queued.remove(queued.size() - 1);
                    inflight.add(url);
                    return url;
                }
            }

            @Override
            public void run() {
                String url = next();
                while (url != null) {
                    try {
                        Glide.with(applicationContext)
                                .downloadOnly()
                                .priority(Priority.LOW)
                                .load(url)
                                .submit()
                                .get();

                        synchronized (applicationContext) {
                            inflight.remove(url);
                            completed.add(url);
                            while (completed.size() > PREDOWNLOAD_COUNT * 4) {
                                completed.remove(0);
                            }
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        synchronized (applicationContext) {
                            threads[index] = null;
                            return;
                        }
                    }

                    url = next();
                }
            }
        }
    }

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
        Predownloader predownloader = Predownloader.getInstance(holder.imageView.getContext());
        for (int i = position + 1; i <= position + Predownloader.PREDOWNLOAD_COUNT && i < getItemCount(); i++) {
            predownloader.predownload(getItem(i));
        }
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

    @Override
    public void onViewRecycled(@NonNull WallpaperViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.imageView.getContext()).clear(holder.imageView);
    }

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
                        .priority(Priority.IMMEDIATE)
                        .override(wallpaper.thumbnail.width, wallpaper.thumbnail.height)
                        .into(imageView);
            }
        }
    }
}