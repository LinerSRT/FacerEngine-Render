package com.liner.facerengineview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.liner.facerengineview.Engine.RenderView;

import java.io.File;
import java.util.Objects;

public class WatchFaceViewActivity extends AppCompatActivity {
    private RenderView renderView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_watch_face_view);
        renderView = findViewById(R.id.renderView);
        Intent intent = getIntent();
        final File faceFile = new File(Objects.requireNonNull(intent.getStringExtra("skinpath")));
        new FileReader(this,faceFile).read(new FileReader.IFileReader() {
            @Override
            public void onReadComplete(File file, FileReader.SKIN_FORMAT skinFormat, Bitmap preview) {
                switch (skinFormat){
                    case CLOCKSKIN:
                        renderView.initClockSkin(file);
                        renderView.startDraw(10);
                        break;
                    case FACER:
                        renderView.init(file);
                        renderView.startDraw(10);
                        break;
                }
            }
            @Override
            public void onReadError(String reason) {
                Log.e("Reader", "Read file error, reason"+reason);
            }
        });


    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }
}
