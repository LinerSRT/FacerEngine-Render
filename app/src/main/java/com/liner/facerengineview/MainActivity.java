package com.liner.facerengineview;

import android.Manifest;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.liner.facerengineview.Engine.RenderView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {
    private Button selectFace;
    private List<SkinHolder> clockList;
    public RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        selectFace = findViewById(R.id.chooseFace);
        clockList = new ArrayList<>();
        searchClockSkinSD(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
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
                            Intent intent = new Intent(MainActivity.this, WatchFaceViewActivity.class);
                            intent.putExtra("skinpath", files[0]);
                            startActivity(intent);
                        }
                    }
                });
                dialog.show();
            }
        });


        recyclerView = findViewById(R.id.chooseRecycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        SkinChooseAdapter clockSkinChooseAdapter = new SkinChooseAdapter(this, clockList);
        recyclerView.setAdapter(clockSkinChooseAdapter);
    }


    public void searchClockSkinSD(File file){
        File[] fileList = file.listFiles();
        if(fileList != null) {
            for (File item : Objects.requireNonNull(file.listFiles())) {
                if (item.isDirectory()) {
                    searchClockSkinSD(item);
                } else {
                    if(isCorrectSkinFile(item)){
                        Log.e("Loader", "Add: "+item.getName());
                        SkinHolder skinHolder = new SkinHolder(item.getAbsolutePath(), getPreview(item));
                        clockList.add(skinHolder);
                    }
                }
            }
        }
    }

    public static boolean isCorrectSkinFile(File file) {
        try {
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith("img_clock_preview.png")) {
                    zip.close();
                    return true;
                } else if(!entry.isDirectory() && entry.getName().endsWith("clock_skin_model.png")){
                    zip.close();
                    return true;
                } else if(!entry.isDirectory() && entry.getName().endsWith("preview.png")){
                    zip.close();
                    return true;
                } else if(!entry.isDirectory() && entry.getName().endsWith("preview.jpg")){
                    zip.close();
                    return true;
                }
            }
            zip.close();
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public Bitmap getPreview(File file){
        try {
            ZipFile zipFile = new ZipFile(file.getAbsolutePath());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while(entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                Bitmap bmp;
                if(!zipEntry.isDirectory())
                if(zipEntry.getName().contains("img_clock_preview") || zipEntry.getName().contains("clock_skin_model") || zipEntry.getName().contains("preview")){
                    bmp = BitmapFactory.decodeStream(zipFile.getInputStream(zipEntry));
                    bmp.setDensity(Resources.getSystem().getDisplayMetrics().densityDpi);
                    return bmp;
                }
            }
            zipFile.close();
        } catch (IOException e){
            return null;
        }
        return null;
    }
}
