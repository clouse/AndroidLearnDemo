package com.clouse.androiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.clouse.androiddemo.imageloader.ImageLoaderSampleActivity;

public class MainActivity extends Activity implements View.OnClickListener {
//aa
    private Button imageLoaderDemoBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
        imageLoaderDemoBtn.setOnClickListener(this);
    }

    /**
     * 初始化控件
     */
    private void initView() {
        imageLoaderDemoBtn = (Button) findViewById(R.id.btn_image_loader_demo);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_image_loader_demo:
                startActivity(new Intent(MainActivity.this, ImageLoaderSampleActivity.class));
                break;
        }
    }
}
