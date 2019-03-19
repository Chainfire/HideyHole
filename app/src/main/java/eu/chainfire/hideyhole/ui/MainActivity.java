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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import eu.chainfire.hideyhole.R;
import eu.chainfire.hideyhole.api.WallpaperResponse;
import eu.chainfire.hideyhole.data.WallpaperAdapter;
import eu.chainfire.hideyhole.data.WallpaperDataSource;
import eu.chainfire.hideyhole.data.WallpaperViewModel;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_FILTER_SORT = "FILTER_SORT";
    private static final String PREF_FILTER_DEVICE = "FILTER_DEVICE";
    private static final String PREF_FILTER_CATEGORY = "FILTER_CATEGORY";

    private SharedPreferences prefs;
    private WallpaperAdapter adapter;
    private WallpaperViewModel wallpaperViewModel;

    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        
        WallpaperDataSource.setFilterSort(prefs.getString(PREF_FILTER_SORT, WallpaperDataSource.SORT_DEFAULT));
        WallpaperDataSource.setFilterDevice(prefs.getString(PREF_FILTER_DEVICE, WallpaperDataSource.DEVICE_DEFAULT));
        WallpaperDataSource.setFilterCategory(prefs.getString(PREF_FILTER_CATEGORY, WallpaperDataSource.CATEGORY_DEFAULT));
        refreshLayout = findViewById(R.id.refresh);
        refreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent, getTheme()));
        refreshLayout.setOnRefreshListener(() -> getDataSource().invalidate());
        progressBar = findViewById(R.id.progress_bar);

        adapter = new WallpaperAdapter();

        wallpaperViewModel = ViewModelProviders.of(this).get(WallpaperViewModel.class);
        wallpaperViewModel.getLivePagedList().observe(this, wallpapers -> {
            adapter.submitList(wallpapers);
            progressBar.setVisibility(View.GONE);
            refreshLayout.setRefreshing(false);
        });

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

    private WallpaperDataSource getDataSource() {
        return wallpaperViewModel.getFactory().getMutableLiveData().getValue();
    }

    private void applySelection(String filter) {
        if (filter.startsWith("device:")) {
            WallpaperDataSource.setFilterDevice(filter.substring("device:".length()));
        } else if (filter.startsWith("category:")) {
            WallpaperDataSource.setFilterCategory(filter.substring("category:".length()));
        } else if (filter.startsWith("sort:")) {
            WallpaperDataSource.setFilterSort(filter.substring("sort:".length()));
        } else {
            return;
        }
        getDataSource().invalidate();

        prefs.edit()
                .putString(PREF_FILTER_SORT, WallpaperDataSource.getFilterSort())
                .putString(PREF_FILTER_DEVICE, WallpaperDataSource.getFilterDevice())
                .putString(PREF_FILTER_CATEGORY, WallpaperDataSource.getFilterCategory())
                .apply();
    }

    private MenuItem addMenuSelectionOption(Menu parent, int groupId, int itemId, int order, int text, String selection, String matchForCheck) {
        MenuItem item = parent.add(groupId, itemId, order, text);
        boolean checked = (selection.equals(matchForCheck));
        item.setCheckable(checked);
        item.setChecked(checked);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        item.setOnMenuItemClickListener(which -> {
            which.setCheckable(true);
            which.setChecked(true);
            applySelection(selection);
            for (int i = 0; i < parent.size(); i++) {
                MenuItem other = parent.getItem(i);
                if (other != which) {
                    other.setCheckable(false);
                    other.setChecked(false);

                }
            }
            return true;
        });
        return item;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu target;

        String device = "device:" + getDataSource().getFilterDevice();
        String category = "category:" + getDataSource().getFilterCategory();
        String sort = "sort:" + getDataSource().getFilterSort();

        target = menu.addSubMenu(R.string.menu_sort_title);
        target.setGroupDividerEnabled(true);
        addMenuSelectionOption(target, 1, 101, 101, R.string.menu_sort_popular, "sort:popular", sort);
        addMenuSelectionOption(target, 1, 102, 102, R.string.menu_sort_new, "sort:new", sort);

        target = menu.addSubMenu(R.string.menu_device_title);
        target.setGroupDividerEnabled(true);
        addMenuSelectionOption(target, 1, 101, 101, R.string.menu_device_all, "device:*", device);
        addMenuSelectionOption(target, 2, 201, 201, R.string.menu_device_s10, "device:s10", device);
        addMenuSelectionOption(target, 2, 202, 202, R.string.menu_device_s10e, "device:s10e", device);
        addMenuSelectionOption(target, 2, 203, 203, R.string.menu_device_s10plus, "device:s10plus", device);

        target = menu.addSubMenu(R.string.menu_category_title);
        target.setGroupDividerEnabled(true);
        addMenuSelectionOption(target, 1, 101, 101, R.string.menu_category_all, "category:*", category);
        addMenuSelectionOption(target, 2, 201, 201, R.string.menu_category_cutouts, "category:*cutout", category);
        addMenuSelectionOption(target, 2, 201, 201, R.string.menu_category_amoleddark, "category:amoleddark", category);
        addMenuSelectionOption(target, 2, 202, 202, R.string.menu_category_abstract, "category:abstract", category);
        addMenuSelectionOption(target, 2, 203, 203, R.string.menu_category_anime, "category:anime", category);
        addMenuSelectionOption(target, 2, 204, 204, R.string.menu_category_artwork, "category:artwork", category);
        addMenuSelectionOption(target, 2, 205, 205, R.string.menu_category_minimalistic, "category:minimalistic", category);
        addMenuSelectionOption(target, 2, 206, 206, R.string.menu_category_nature, "category:nature", category);
        addMenuSelectionOption(target, 2, 207, 207, R.string.menu_category_person, "category:person", category);
        addMenuSelectionOption(target, 2, 208, 208, R.string.menu_category_urban, "category:urban", category);
        addMenuSelectionOption(target, 2, 209, 209, R.string.menu_category_other, "category:other", category);

        menu.add(R.string.menu_reddit).setOnMenuItemClickListener(which -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/S10wallpapers/")));
            return true;
        });

        return true;
    }
}
