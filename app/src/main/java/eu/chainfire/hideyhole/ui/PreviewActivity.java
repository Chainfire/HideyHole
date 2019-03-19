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

import android.Manifest;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.transition.Transition;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import eu.chainfire.hideyhole.BuildConfig;
import eu.chainfire.hideyhole.R;
import eu.chainfire.hideyhole.api.WallpaperResponse;

public class PreviewActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    private static final int BRIGHTNESS_DEFAULT = 100;
    private static final int BRIGHTNESS_DEFAULT_2 = 75;
    private static final int CONTRAST_DEFAULT = 0;
    private static final int BLACKPOINT_DEFAULT = 0;
    private static final int SATURATION_DEFAULT = 100;

    // Used for passing data from MainActivity, avoiding passing large Bitmaps through Intents/Binder.
    private static Drawable nextDrawable = null;
    private static WallpaperResponse.Wallpaper nextWallpaper = null;
    public static void setNextWallpaper(Drawable drawable, WallpaperResponse.Wallpaper wallpaper) {
        nextDrawable = drawable;
        nextWallpaper = wallpaper;
    }

    // Translate methods should really be part of the original ColorMatrix...
    private class ColorMatrixTranslate extends ColorMatrix {
        void setScale(float rgbScale) {
            setScale(rgbScale, rgbScale, rgbScale, 1.0f);
        }

        void setTranslate(float rgbTranslate) {
            float[] array = getArray();
            array[4] = rgbTranslate;
            array[9] = rgbTranslate;
            array[14] = rgbTranslate;
            array[19] = 0f;
            set(array);
        }
    }

    private CameraCutout cameraCutout;
    private ImageView imageBackground;
    private SeekBar seekBrightness;
    private SeekBar seekContrast;
    private SeekBar seekBlackpoint;
    private SeekBar seekSaturation;

    private ColorMatrixTranslate cmBrightness = new ColorMatrixTranslate();
    private ColorMatrixTranslate cmContrast = new ColorMatrixTranslate();
    private ColorMatrixTranslate cmBlackpoint = new ColorMatrixTranslate();
    private ColorMatrixTranslate cmSaturation = new ColorMatrixTranslate();
    private ColorMatrix cmFinal = new ColorMatrix();

    private WallpaperResponse.Wallpaper wallpaper = null;
    private Bitmap bitmap = null;
    private Bitmap scaled = null;
    private ReentrantLock lock = new ReentrantLock(true);

    private boolean entryAnimating = true;
    private boolean entryUpdatedImage = false;

    private ConstraintLayout panelInfo;
    private ImageButton btnEdit;
    private ImageButton btnSave;

    private SaveTask saveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_preview);

        // get passed data

        if ((nextDrawable == null) || (nextWallpaper == null)) return;
        imageBackground = findViewById(R.id.background);
            // we create a copy of the thumbnail drawable, otherwise the reverse transition flickers
            // if we have not loaded the high-res image yet... don't know why
        imageBackground.setImageDrawable(nextDrawable.getConstantState().newDrawable().mutate());
        wallpaper = nextWallpaper;
        nextDrawable = null;
        nextWallpaper = null;

        // setup ui
        
        panelInfo = findViewById(R.id.info_panel);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);

        panelInfo.setVisibility(View.GONE);

        final SeekBar.OnSeekBarChangeListener onSeekBarChange = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                applyColorFilter();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekBrightness = findViewById(R.id.seek_brightness);
        seekBrightness.setMin(50);
        seekBrightness.setMax(100);
        seekBrightness.setProgress(BRIGHTNESS_DEFAULT);
        seekBrightness.setOnSeekBarChangeListener(onSeekBarChange);

        seekContrast = findViewById(R.id.seek_contrast);
        seekContrast.setMin(-50);
        seekContrast.setMax(50);
        seekContrast.setProgress(CONTRAST_DEFAULT);
        seekContrast.setOnSeekBarChangeListener(onSeekBarChange);

        seekBlackpoint = findViewById(R.id.seek_blackpoint);
        seekBlackpoint.setMin(-50);
        seekBlackpoint.setMax(50);
        seekBlackpoint.setProgress(BLACKPOINT_DEFAULT);
        seekBlackpoint.setOnSeekBarChangeListener(onSeekBarChange);

        seekSaturation = findViewById(R.id.seek_saturation);
        seekSaturation.setMin(0);
        seekSaturation.setMax(200);
        seekSaturation.setProgress(SATURATION_DEFAULT);
        seekSaturation.setOnSeekBarChangeListener(onSeekBarChange);

        setInputEnabled(false);

        // apply wallpaper info

        Glide.with(this)
                .load(wallpaper.image.url)
                .into(new CustomTarget<Drawable>() {
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Toast.makeText(PreviewActivity.this, R.string.network_failure, Toast.LENGTH_LONG).show();
                        PreviewActivity.this.finish();
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        lock.lock();
                        try {
                            entryUpdateImage(resource);
                        } finally {
                            lock.unlock();
                        }
                    }
                });

        ((TextView)findViewById(R.id.title)).setText(wallpaper.title);
        ((TextView)findViewById(R.id.author)).setText(wallpaper.author);

        getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
            @Override public void onTransitionStart(Transition transition) {}
            @Override public void onTransitionCancel(Transition transition) {}
            @Override public void onTransitionPause(Transition transition) {}
            @Override public void onTransitionResume(Transition transition) {}

            @Override
            public void onTransitionEnd(Transition transition) {
                lock.lock();
                try {
                    entryAnimating = false;
                    entryUpdateImage(null);
                } finally {
                    lock.unlock();
                }
            }
        });

        applyColorFilter();

        // determine camera cutout area
        cameraCutout = new CameraCutout(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_layout), (view, insets) -> {
            cameraCutout.updateFromInsets(insets);

            if (BuildConfig.DEBUG) {
                Rect r = cameraCutout.getCutout().getArea();
                if (r != null) {
                    View v = findViewById(R.id.cutout);
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)v.getLayoutParams();
                    params.width = r.width();
                    params.height = r.height();
                    params.setMargins(r.left, r.top, 0, 0);
                    v.setLayoutParams(params);

                    ((TextView)findViewById(R.id.title)).setText(String.format(Locale.ENGLISH, "cut:%d:%d:%d:%d",
                            r.left, r.top, r.right, r.bottom
                    ));
                    ((TextView)findViewById(R.id.author)).setText(String.format(Locale.ENGLISH, "n:%dx%d c:%dx%d",
                            cameraCutout.getNativeResolution().x, cameraCutout.getNativeResolution().y,
                            cameraCutout.getCurrentResolution().x, cameraCutout.getCurrentResolution().y
                    ));
                }
            }

            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fadeInfoPanel(true);
    }

    @Override
    protected void onStop() {
        panelInfo.setVisibility(View.GONE); // required for fade in on task switch
        super.onStop();
    }
    
    private void applyColorFilter() {
        // translate our seekbar to image manipulations

        float scale;
        float translate;

        // brightness; scale (keep 0, move 255)
        scale = (float)seekBrightness.getProgress() / 100f;
        cmBrightness.reset();
        cmBrightness.setScale(scale);

        // contrast; scale from center (keep 128, move surrounding values closer/further)
        scale = 1.0f + (float)seekContrast.getProgress() / 100f;
        translate = (-0.5f * scale + 0.5f) * 255f;
        cmContrast.reset();
        cmContrast.setScale(scale);
        cmContrast.setTranslate(translate);

        // black point; scale from right (move 0, keep 255)
        scale = 1.0f - (float)seekBlackpoint.getProgress() / 100f;
        translate = (-1.0f * (scale - 1.0f)) * 255f;
        cmBlackpoint.reset();
        cmBlackpoint.setScale(scale);
        cmBlackpoint.setTranslate(translate);

        // saturation
        cmSaturation.reset();
        cmSaturation.setSaturation((float)seekSaturation.getProgress() / 100f);

        // combine
        cmFinal.reset();
        cmFinal.postConcat(cmBrightness);
        cmFinal.postConcat(cmContrast);
        cmFinal.postConcat(cmBlackpoint);
        cmFinal.postConcat(cmSaturation);

        // apply
        imageBackground.setColorFilter(new ColorMatrixColorFilter(cmFinal));
    }

    private void setInputEnabled(boolean enable) {
        btnEdit.setEnabled(enable);
        btnEdit.setClickable(enable);
        btnSave.setEnabled(enable);
        btnSave.setClickable(enable);
        seekBrightness.setEnabled(enable);
        seekContrast.setEnabled(enable);
        seekBlackpoint.setEnabled(enable);
        seekSaturation.setEnabled(enable);
    }

    private void setProgressVisible(boolean visible) {
        findViewById(R.id.progress_container).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void fadeInfoPanel(final boolean visible) {
        if (visible && (panelInfo.getVisibility() == View.VISIBLE)) return;
        if (entryAnimating) return;

        AlphaAnimation fade = new AlphaAnimation(visible ? 0f : 1f, visible ? 1f : 0f);
        fade.setDuration(250);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                panelInfo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                panelInfo.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            @Override public void onAnimationRepeat(Animation animation) {}
        });
        panelInfo.startAnimation(fade);
    }

    private void entryUpdateImage(Drawable drawable) {
        // delayed update of high-res image, required to keep activity entry animation
        // (which uses the thumbnail) smooth

        if (drawable != null) {
            this.bitmap = ((BitmapDrawable)drawable).getBitmap();
        }
        if (this.bitmap != null) {
            if (!entryAnimating && !entryUpdatedImage) {
                imageBackground.setImageBitmap(this.bitmap);
                entryUpdatedImage = true;
                fadeInfoPanel(true);
            }
            setProgressVisible(false);
            setInputEnabled(true);
        }
    }

    public void onBrightnessResetClick(View v) {
        if (seekBrightness.getProgress() == BRIGHTNESS_DEFAULT) {
            seekBrightness.setProgress(BRIGHTNESS_DEFAULT_2);
        } else {
            seekBrightness.setProgress(BRIGHTNESS_DEFAULT);
        }
        applyColorFilter();
    }

    public void onContrastResetClick(View v) {
        seekContrast.setProgress(CONTRAST_DEFAULT);
        applyColorFilter();
    }

    public void onBlackpointResetClick(View v) {
        seekBlackpoint.setProgress(BLACKPOINT_DEFAULT);
        applyColorFilter();
    }

    public void onSaturationResetClick(View v) {
        seekSaturation.setProgress(SATURATION_DEFAULT);
        applyColorFilter();
    }

    private void scale(CameraCutout.Cutout src, CameraCutout.Cutout dst, Point res) {
        // scale to res
        src = src.scaleTo(res);
        dst = dst.scaleTo(res);

        if (!BuildConfig.DEBUG) { // in debug mode we may want to adjust the current cutout for
                                  // testing, and this code would be annoying in that case
            if (src.equalsScaled(dst)) {
                // including room for rounding errors at different resolutions, it seems the
                // source and target gaps are the same, do nothing; scaling in this case may
                // actually _introduce_ error.
                this.scaled = null;
                imageBackground.setImageBitmap(bitmap);
                return;
            }
        }

        // make sure our bitmap is fullscreen (only needed if we apply this scaling/realignment)
        if ((bitmap.getWidth() != res.x) || (bitmap.getHeight() != res.y)) {
            Bitmap scaled = Bitmap.createBitmap(res.x, res.y, Bitmap.Config.ARGB_8888, false);
            Canvas canvas = new Canvas(scaled);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, res.x, res.y), paint);
            bitmap.recycle();
            bitmap = scaled;
        }

        // to reduce upscaling, if dst size is smaller than src size, we scale down
        // src and position it closest to dest
        if (dst.getArea().width() < src.getArea().width()) {
            Rect sA = src.getArea();
            Rect dA = dst.getArea();
            int w = dA.width();
            if (dA.left <= sA.left) {
                sA.right = sA.left + w; // anchor left
            } else if (dA.right >= sA.right) {
                sA.left = sA.right - w; // anchor right
            } else {
                sA.left = dA.centerX() - (w / 2); // center
            }
            src = new CameraCutout.Cutout(sA, src.getResolution());
        }
        if (dst.getArea().height() < src.getArea().height()) {
            Rect sA = src.getArea();
            Rect dA = dst.getArea();
            int h = dA.height();
            if (dA.top <= sA.top) {
                sA.bottom = sA.top + h; // anchor top
            } else if (dA.bottom >= sA.bottom) {
                sA.top = sA.bottom - h; // anchor bottom
            } else {
                sA.top = dA.centerY() - (h / 2); // center
            }
            src = new CameraCutout.Cutout(sA, src.getResolution());
        }

        // perform image scaling
        scaleFull(src, dst, res, true);

        //TODO logic for going from S10/S10E to S10PLUS, currently just zooms a lot
    }

    private float scaleFull(CameraCutout.Cutout src, CameraCutout.Cutout dst, Point res, boolean apply) {
        // - image must be the correct aspect ratio and resolution for this to work
        // - this scales the holes to fit

        RectF srcHole = new RectF(src.getArea());
        RectF dstHole = new RectF(dst.getArea());
        PointF srcRes = new PointF(res);
        RectF scaledArea = new RectF(src.getArea());
        PointF scaledRes = new PointF(res);

        // find scale for the old hole area to cover the new hole area
        float scale = Math.max(
                dstHole.height() / srcHole.height(),
                dstHole.width() / srcHole.width()
        );

        // scale to cover gaps (black bars on top, left, right, bottom) after previous scale
        scale = Math.max(
                scale,
                Math.max(
                    Math.max(
                            dstHole.centerX() / (srcHole.centerX() * scale),  // left
                            dstHole.centerY() / (srcHole.centerY() * scale)   // top
                    ),
                    Math.max(
                            (res.x - dstHole.centerX()) / ((res.x - srcHole.centerX()) * scale),  // right
                            (res.y - dstHole.centerY()) / ((res.y - srcHole.centerY()) * scale)   // bottom
                    )
                )
        );

        // perform scale and re-center
        scaledRes.set(
            srcRes.x * scale,
            srcRes.y * scale
        );

        scaledArea.left = dstHole.centerX() - (srcHole.centerX() * scale);
        scaledArea.top = dstHole.centerY() - (srcHole.centerY() * scale);
        scaledArea.right = scaledArea.left + scaledRes.x;
        scaledArea.bottom = scaledArea.top + scaledRes.y;

        if (apply) {
            Bitmap scaled = Bitmap.createBitmap(res.x, res.y, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaled);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(bitmap, new Rect(0, 0, res.x, res.y), scaledArea, paint);
            this.scaled = scaled;
            imageBackground.setImageBitmap(scaled);
        }

        return scale;
    }

    public void onEditClick(View v) {
        (new AlertDialog.Builder(this))
                .setTitle(R.string.scale_source_message)
                .setItems(new CharSequence[] {
                        getString(R.string.scale_source_s10),
                        getString(R.string.scale_source_s10e),
                        getString(R.string.scale_source_s10plus)
                }, (dialog, which) -> {
                    CameraCutout.Cutout source = null;
                    switch (which) {
                        case 0:
                            source = CameraCutout.CUTOUT_S10;
                            break;
                        case 1:
                            source = CameraCutout.CUTOUT_S10E;
                            break;
                        case 2:
                            source = CameraCutout.CUTOUT_S10PLUS;
                            break;
                    }
                    if (source != null) {
                        scale(source, cameraCutout.getCutout(), cameraCutout.getCurrentResolution());
                    }
                })
                .show();
    }

    public void onDownloadClick(View v) {
        saveTask = new SaveTask(this, wallpaper, cmFinal, scaled == null ? bitmap : scaled, SaveTask.SaveTarget.DOWNLOAD);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            saveTask.execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveTask.execute();
            }
        }
    }

    public void onSaveClick(View v) {
        saveTask = new SaveTask(this, wallpaper, cmFinal, scaled == null ? bitmap : scaled, SaveTask.SaveTarget.WALLPAPER);
        saveTask.execute();
    }

    private static class SaveTask extends AsyncTask<Void, Void, Bitmap> {
        public enum SaveTarget { WALLPAPER, DOWNLOAD }

        private final WeakReference<PreviewActivity> activity;
        private final WallpaperResponse.Wallpaper wallpaper;
        private final ColorMatrix colorMatrix;
        private final Bitmap source;
        private final SaveTarget target;

        public SaveTask(PreviewActivity activity, WallpaperResponse.Wallpaper wallpaper, ColorMatrix colorMatrix, Bitmap source, SaveTarget target) {
            this.activity = new WeakReference<>(activity);
            this.wallpaper = wallpaper;
            this.colorMatrix = colorMatrix;
            this.source = source;
            this.target = target;
        }

        @Override
        protected void onPreExecute() {
            PreviewActivity activity = this.activity.get();
            if (activity != null) {
                activity.applyColorFilter();
                activity.setProgressVisible(true);
                activity.setInputEnabled(false);
            }
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap dest = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888, false);
            Canvas canvas = new Canvas(dest);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setFilterBitmap(true);
            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            canvas.drawBitmap(source, 0, 0, paint);
            return dest;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            PreviewActivity activity = this.activity.get();
            if (activity != null) {
                if (target == SaveTarget.WALLPAPER) {
                    (new AlertDialog.Builder(activity))
                            .setMessage(R.string.wallpaper_target_message)
                            .setNeutralButton(R.string.wallpaper_target_both, (dialog, which) -> applyWallpaper(bitmap, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK))
                            .setNegativeButton(R.string.wallpaper_target_home, (dialog, which) -> applyWallpaper(bitmap, WallpaperManager.FLAG_SYSTEM))
                            .setPositiveButton(R.string.wallpaper_target_lock, (dialog, which) -> applyWallpaper(bitmap, WallpaperManager.FLAG_LOCK))
                            .show();
                } else if (target == SaveTarget.DOWNLOAD) {
                    saveWallpaper(bitmap);
                }
                activity.setProgressVisible(false);
                activity.setInputEnabled(true);
            }
        }

        private void applyWallpaper(Bitmap bitmap, int which) {
            PreviewActivity activity = this.activity.get();
            if (activity != null) {
                try {
                    WallpaperManager.getInstance(activity).setBitmap(bitmap, null, true, which);

                    // FLAG_ACTIVITY_CLEAR_TASK causes home screen reload, but else we may return to the app drawer :(
                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(home);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void saveWallpaper(Bitmap bitmap) {
            PreviewActivity activity = this.activity.get();
            if (activity != null) {
                String filename = ("HideyHole_" + wallpaper.title + "_by_" + wallpaper.author).toLowerCase(Locale.ENGLISH).replaceAll("\\W+", "_") + ".png";
                while (filename.contains("__")) {
                    filename = filename.replace("__", "_");
                }

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                if (file.exists()) file.delete();
                try {
                    FileOutputStream os = new FileOutputStream(file);
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    } finally {
                        os.close();
                    }

                    DownloadManager downloadManager = (DownloadManager)activity.getSystemService(DOWNLOAD_SERVICE);
                    downloadManager.addCompletedDownload(
                            wallpaper.title,
                            wallpaper.title + " by " + wallpaper.author + " (Hidey Hole)",
                            true,
                            "image/png",
                            file.getAbsolutePath(),
                            file.length(),
                            true
                    );

                    activity.finish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
