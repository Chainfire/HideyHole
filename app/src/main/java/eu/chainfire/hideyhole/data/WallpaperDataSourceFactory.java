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

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import eu.chainfire.hideyhole.api.WallpaperResponse;

public class WallpaperDataSourceFactory extends DataSource.Factory {
    private MutableLiveData<WallpaperDataSource> mutableLiveData = new MutableLiveData<>();

    @NonNull
    @Override
    public DataSource<Integer, WallpaperResponse.Wallpaper> create() {
        WallpaperDataSource dataSource = new WallpaperDataSource();
        mutableLiveData.postValue(dataSource);
        return dataSource;
    }

    public MutableLiveData<WallpaperDataSource> getMutableLiveData() {
        return mutableLiveData;
    }
}