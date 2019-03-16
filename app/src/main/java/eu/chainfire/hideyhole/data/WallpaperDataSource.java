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
    private static final int PAGE_FIRST = 1;
    public static final int PAGE_SIZE = 50;

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, WallpaperResponse.Wallpaper> callback) {
        RetrofitClient.getInstance()
                //.getApi().getAnswers(PAGE_FIRST, PAGE_SIZE, SITE_NAME) //TODO
                .getApi().getWallpapers()
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        if (response.body() != null) {
                            callback.onResult(response.body().wallpapers, null, null /*PAGE_FIRST + 1*/);  //TODO
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
                //.getApi().getAnswers(params.key, PAGE_SIZE, SITE_NAME) //TODO
                .getApi().getWallpapers()
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        //if the current page is greater than one
                        //we are decrementing the page number
                        //else there is no previous page
                        Integer adjacentKey = (params.key > 1) ? params.key - 1 : null;
                        if (response.body() != null) {
                            //passing the loaded data
                            //and the previous page key
                            callback.onResult(response.body().wallpapers, adjacentKey);
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
                .getApi().getWallpapers()
                //.getAnswers(params.key, PAGE_SIZE, SITE_NAME) //TODO
                .enqueue(new Callback<WallpaperResponse>() {
                    @Override
                    public void onResponse(Call<WallpaperResponse> call, Response<WallpaperResponse> response) {
                        if (response.body() != null) {
                            //if the response has next page
                            //incrementing the next page number
                            //Integer key = response.body().has_more ? params.key + 1 : null; //TODO
                            Integer key = null;

                            //passing the loaded data and next page value
                            callback.onResult(response.body().wallpapers, key);
                        }
                    }

                    @Override
                    public void onFailure(Call<WallpaperResponse> call, Throwable t) {
                    }
                });
    }
}