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

package eu.chainfire.hideyhole.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.hideyhole.R;
import eu.chainfire.hideyhole.api.WallpaperResponse;
import eu.chainfire.hideyhole.data.WallpaperAdapter;
import eu.chainfire.hideyhole.data.WallpaperViewModel;

public class MainActivity extends AppCompatActivity {
    private WallpaperAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false));
        recyclerView.setHasFixedSize(true);

        adapter = new WallpaperAdapter();

        WallpaperViewModel wallpaperViewModel = ViewModelProviders.of(this).get(WallpaperViewModel.class);
        wallpaperViewModel.getLivePagedList().observe(this, adapter::submitList);

        recyclerView.setAdapter(adapter);
    }

    private WallpaperAdapter.Listener onAdapterClickListener = new WallpaperAdapter.Listener() {
        @Override
        public void onClick(WallpaperResponse.Wallpaper wallpaper, ImageView imageView) {
            Intent i = new Intent(MainActivity.this, PreviewActivity.class);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, imageView, "wallpaperExplode");
            PreviewActivity.setNextWallpaper(imageView.getDrawable(), wallpaper); // bypass Binder
            startActivity(i, options.toBundle());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setListener(onAdapterClickListener);
    }

    @Override
    protected void onPause() {
        adapter.setListener(null);
        super.onPause();
    }
}
