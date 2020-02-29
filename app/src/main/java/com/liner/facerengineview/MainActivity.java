package com.liner.facerengineview;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.liner.facerengineview.Engine.RenderFacerView;
import com.liner.facerengineview.Engine.Util.UnCompress;
import com.liner.facerengineview.Engine.Util.WatchFaceData;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private RenderFacerView facerView;
    private Button selectFace, startDraw, stopDraw, switchAmbient, switchMode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferenceManager = PreferenceManager.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        facerView = findViewById(R.id.renderFacer);
        selectFace = findViewById(R.id.chooseFace);
        startDraw = findViewById(R.id.startDraw);
        stopDraw = findViewById(R.id.stopDraw);
        switchAmbient = findViewById(R.id.switchAmbient);
        switchMode = findViewById(R.id.switchMode);
        if(!preferenceManager.getString("last_used_watchface", "err").equals("err")){
            facerView.init(new WatchFaceData(this,preferenceManager.getString("last_used_watchface", "err")));
            facerView.startDraw(10);
        }
        selectFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(Environment.getExternalStorageDirectory()+File.separator);
                properties.error_dir = new File(Environment.getExternalStorageDirectory()+File.separator);
                properties.offset = new File(Environment.getExternalStorageDirectory()+File.separator);
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
                dialog.setTitle("Choose facer skin");
                dialog.setNegativeBtnName("Cancel");
                dialog.setPositiveBtnName("Choose");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(final String[] files) {
                        if(files.length != 0) {
                            final File faceFile = new File(files[0]);
                            final String faceName = faceFile.getName().replaceFirst("[.][^.]+$", "");
                            new UnCompress(getCacheDir() + File.separator + "Facer/" + faceName + "/", files[0], new UnCompress.OnCompleteListener() {
                                @Override
                                public void onComplete() {
                                    preferenceManager.saveString("last_used_watchface", faceName);
                                    facerView.init(new WatchFaceData(MainActivity.this, faceName));
                                    facerView.startDraw(10);
                                }

                                @Override
                                public void onError() {

                                    Log.e("PARSER", "FAILED UNPACK");
                                }
                            }).unzip();
                        }
                    }
                });
                facerView.stopDraw();
                dialog.show();
            }
        });
        startDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                facerView.startDraw();
            }
        });
        stopDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                facerView.stopDraw();
            }
        });
        switchAmbient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                facerView.setLowPower(!facerView.isLowPower());
            }
        });
        switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                facerView.setRoundWatch(!facerView.isRoundWatch());
            }
        });
    }
}
