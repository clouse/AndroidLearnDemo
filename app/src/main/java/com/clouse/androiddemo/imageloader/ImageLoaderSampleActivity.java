package com.clouse.androiddemo.imageloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.clouse.androiddemo.R;
import com.clouse.androiddemo.imageloader.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageLoaderSampleActivity extends Activity {
    private GridView mGridView;
    private RelativeLayout mBottomLy;
    private List<String> mImgs;
    private TextView mDirName;
    private TextView mDirCount;
    private File mCurrentFile;
    private int mMaxCount;
    private ProgressDialog mProgressDialog;
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x100) {
                mProgressDialog.dismiss();
                dataToView();
            }
        }
    };

    private void dataToView() {
        if (mCurrentFile == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_LONG).show();
            return;
        } else {
            mImgs = Arrays.asList(mCurrentFile.list());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_loader_sample);
        initView();
        initDatas();
        initEvents();
    }

    private void initEvents() {
    }

    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this,"当前SD卡不可用",Toast.LENGTH_LONG).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this,null,"正在加载...");
        new Thread(){
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = ImageLoaderSampleActivity.this.getContentResolver();
                String selection = MediaStore.Images.Media.MIME_TYPE+"= ? or"+
                        MediaStore.Images.Media.MIME_TYPE+"= ? or";
                String[] selectionArgs = new String[]{"image/png","image/jpeg"};
                Cursor cursor = cr.query(mImgUri,null,selection,selectionArgs,MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;//部分机型有问题
                    }
                    String dirPath = parentFile.getAbsolutePath();
                    if(mDirPaths.contains(dirPath)){
                        continue;
                    }
                    FolderBean bean = new FolderBean();
                    bean.setDir(dirPath);
                    bean.setFirstImgPath(path);
                    if (parentFile.list() == null) {
                        continue;
                    }
                    int imgSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            if (s.endsWith(".jpg") || s.endsWith(".png") || s.endsWith(".jpeg")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    bean.setImgCount(imgSize);
                    mFolderBeans.add(bean);
                    if (imgSize > mMaxCount) {
                        mCurrentFile = parentFile;
                        mMaxCount = imgSize;
                    }
                }
                cursor.close();
                mHandler.sendEmptyMessage(0x100);
            }
        }.start();

    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_grid_view);
        mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }
}
