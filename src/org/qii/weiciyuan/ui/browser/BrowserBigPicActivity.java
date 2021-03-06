package org.qii.weiciyuan.ui.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.support.file.FileDownloaderHttpHelper;
import org.qii.weiciyuan.support.imagetool.ImageTool;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.AppLogger;
import org.qii.weiciyuan.ui.Abstract.AbstractAppActivity;
import org.qii.weiciyuan.ui.main.MainTimeLineActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * User: qii
 * Date: 12-8-18
 */
public class BrowserBigPicActivity extends AbstractAppActivity {

    private String url;
    private WebView webView;
    private ProgressBar pb;
    private FrameLayout fl;
    private PicSimpleBitmapWorkerTask task;
    private PicSaveTask saveTask;
    private String path;
    private ShareActionProvider mShareActionProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_listview_pic_big_layout);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.browser_picture);


        pb = (ProgressBar) findViewById(R.id.pb);
        fl = (FrameLayout) findViewById(R.id.fl);

        webView = (WebView) findViewById(R.id.iv);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setBackgroundColor(getResources().getColor(R.color.transparent));

        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        url = getIntent().getStringExtra("url");
        if (task == null || task.getStatus() == MyAsyncTask.Status.FINISHED) {
            task = new PicSimpleBitmapWorkerTask();
            task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu_browserbigpicactivity, menu);
        MenuItem item = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainTimeLineActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            case R.id.menu_save:
                if (task != null && task.getStatus() == MyAsyncTask.Status.FINISHED) {
                    if (saveTask == null) {
                        saveTask = new PicSaveTask();
                        saveTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                    } else if (saveTask.getStatus() == MyAsyncTask.Status.FINISHED) {
                        Toast.makeText(BrowserBigPicActivity.this, getString(R.string.already_saved), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.menu_share:

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("image/jpeg");

                if (!TextUtils.isEmpty(path)) {
                    Uri uri = Uri.fromFile(new File(path));
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    PackageManager packageManager = getPackageManager();
                    List<ResolveInfo> activities = packageManager.queryIntentActivities(sharingIntent, 0);
                    boolean isIntentSafe = activities.size() > 0;
                    if (isIntentSafe && mShareActionProvider != null) {
                        mShareActionProvider.setShareIntent(sharingIntent);
                    }
                }

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class PicSaveTask extends MyAsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            try {
                MediaStore.Images.Media.insertImage(getContentResolver(), path, "", "");
            } catch (FileNotFoundException e) {
                AppLogger.e(e.getMessage());
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            Toast.makeText(BrowserBigPicActivity.this, getString(R.string.cant_save_pic), Toast.LENGTH_SHORT).show();
            saveTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(BrowserBigPicActivity.this, getString(R.string.save_to_album_successfully), Toast.LENGTH_SHORT).show();
        }


    }

    class PicSimpleBitmapWorkerTask extends MyAsyncTask<String, Integer, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setIndeterminate(true);
        }

        @Override
        protected String doInBackground(String... dd) {

            FileDownloaderHttpHelper.DownloadListener downloadListener = new FileDownloaderHttpHelper.DownloadListener() {
                @Override
                public void pushProgress(int progress, int max) {
                    publishProgress(progress, max);
                }
            };

            if (!isCancelled()) {
                return ImageTool.getLargePictureWithoutRoundedCorner(url, downloadListener);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            int max = values[1];
            pb.setIndeterminate(false);
            pb.setMax(max);
            pb.setProgress(progress);
        }

        @Override
        protected void onCancelled(String bitmap) {
            if (bitmap != null) {


            }

            super.onCancelled(bitmap);
        }

        @Override
        protected void onPostExecute(final String bitmap) {

            if (!TextUtils.isEmpty(bitmap)) {
                path = bitmap;

                pb.setVisibility(View.GONE);

                File file = new File(bitmap);

                AppLogger.e(file.getParent());
                AppLogger.e(file.getName());


                webView.loadDataWithBaseURL("file://" + file.getParent() + "/", "<html style=\"BACKGROUND-COLOR: transparent\"><center><img src=\"" + file.getName() + "\"></BODY></html>", "text/html", "utf-8", "");


                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(bitmap, options);
                int width = options.outWidth;
                int height = options.outHeight;
                getActionBar().setTitle(getActionBar().getTitle() + "(" + String.valueOf(width) + "x" + String.valueOf(height) + ")");


            } else {
                pb.setVisibility(View.GONE);
                int[] attrs = new int[]{R.attr.error};
                TypedArray ta = BrowserBigPicActivity.this.obtainStyledAttributes(attrs);
                Drawable drawableFromTheme = ta.getDrawable(0);
                //                webView.setImageDrawable(drawableFromTheme);
            }

        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (task != null)
            task.cancel(true);
        webView.loadUrl("about:blank");
        webView.stopLoading();
        webView = null;
    }
}
