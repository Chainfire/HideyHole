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
import androidx.paging.PageKeyedDataSource;
import eu.chainfire.hideyhole.api.RetrofitClient;
import eu.chainfire.hideyhole.api.WallpaperResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WallpaperDataSource extends PageKeyedDataSource<Integer, WallpaperResponse.Wallpaper> {
    public static final int PAGE_FIRST = 1;
    public static final int PAGE_SIZE = 100;

    public static final String DEVICE_DEFAULT = "*";
    public static final String CATEGORY_DEFAULT = "*";
    public static final String SORT_DEFAULT = "popular";

    private static String device = DEVICE_DEFAULT;
    private static String category = CATEGORY_DEFAULT;
    private static String sort = SORT_DEFAULT;

    public static String getFilterDevice() { return device; }
    public static void setFilterDevice(String device) { WallpaperDataSource.device = device; }

    public static String getFilterCategory() { return category; }
    public static void setFilterCategory(String category) { WallpaperDataSource.category = category; }

    public static String getFilterSort() { return sort; }
    public static void setFilterSort(String sort) { WallpaperDataSource.sort = sort; }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, WallpaperResponse.Wallpaper> callback) {
        RetrofitClient.getInstance()
                .getApi().getWallpapers(PAGE_FIRST, device, category, sort)
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        if (response.body() != null) {
                            callback.onResult(response.body().results, null, PAGE_FIRST + 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<WallpaperResponse> call, Throwable t) {
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, WallpaperResponse.Wallpaper> callback) {
        RetrofitClient.getInstance()
                .getApi().getWallpapers(params.key, device, category, sort)
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        Integer adjacentKey = (params.key > 1) ? params.key - 1 : null;
                        if (response.body() != null) {
                            callback.onResult(response.body().results, adjacentKey);
                        }
                    }

                    @Override
                    public void onFailure(Call<WallpaperResponse> call, Throwable t) {
                    }
                });
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, WallpaperResponse.Wallpaper> callback) {
        RetrofitClient.getInstance()
                .getApi().getWallpapers(params.key, device, category, sort)
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        if (response.body() != null) {
                            Integer key = response.body().next != null ? params.key + 1 : null;
                            callback.onResult(response.body().results, key);
                        }
                    }

                    @Override
                    public void onFailure(Call<WallpaperResponse> call, Throwable t) {
                    }
                });
    }
}