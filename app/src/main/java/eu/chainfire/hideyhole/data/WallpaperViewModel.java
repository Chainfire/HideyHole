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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import eu.chainfire.hideyhole.api.WallpaperResponse;

public class WallpaperViewModel extends ViewModel {
    private LiveData livePagedList;

    public WallpaperViewModel() {
        WallpaperDataSourceFactory dataSourceFactory = new WallpaperDataSourceFactory();

        PagedList.Config pagedListConfig =
                (new PagedList.Config.Builder())
                        .setEnablePlaceholders(false)
                        .setPageSize(WallpaperDataSource.PAGE_SIZE).build();
 
        livePagedList = (new LivePagedListBuilder(dataSourceFactory, pagedListConfig))
                .build();
    }

    public LiveData<PagedList<WallpaperResponse.Wallpaper>> getLivePagedList() {
        return livePagedList;
    }
}