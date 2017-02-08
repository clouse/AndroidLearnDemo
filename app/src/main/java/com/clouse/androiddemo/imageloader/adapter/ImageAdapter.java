package com.clouse.androiddemo.imageloader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by 浩 on 2017/2/8.
 */

public class ImageAdapter extends BaseAdapter{
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }

    private String mDirPath;
    private List<String> mDatas;
    private LayoutInflater mInflater;
    public ImageAdapter(Context context, List<String> datas, String dirPath) {
        this.mDatas = datas;
        this.mDirPath = dirPath;
        mInflater = LayoutInflater.from(context);
    }
}
