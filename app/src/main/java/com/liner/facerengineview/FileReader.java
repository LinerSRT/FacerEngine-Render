package com.liner.facerengineview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileReader {
    public enum SKIN_FORMAT{
        CLOCKSKIN,
        FACER,
        WATCHMAKER
    }
    private File watchFile;

    public FileReader(File watchFile){
        this.watchFile = watchFile;
    }

    public void read(IFileReader fileReader){
        if(watchFile != null){
            try {
                ZipFile zipFile = new ZipFile(watchFile.getAbsolutePath());
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    switch (zipEntry.getName()) {
                        case "clock_skin.xml":
                            Bitmap preview;
                            try {
                                preview = BitmapFactory.decodeStream(zipFile.getInputStream(zipFile.getEntry("img_clock_preview.png")));
                            } catch (Exception e){
                                preview = BitmapFactory.decodeStream(zipFile.getInputStream(zipFile.getEntry("clock_skin_model.png")));
                            }
                            fileReader.onReadComplete(watchFile, SKIN_FORMAT.CLOCKSKIN, preview);
                            break;
                        case "watchface.json":
                            fileReader.onReadComplete(watchFile, SKIN_FORMAT.FACER, BitmapFactory.decodeStream(zipFile.getInputStream(zipFile.getEntry("preview.png"))));
                            break;
                        case "watch.xml":
                            fileReader.onReadComplete(watchFile, SKIN_FORMAT.WATCHMAKER, BitmapFactory.decodeStream(zipFile.getInputStream(zipFile.getEntry("preview.jpg"))));
                            break;
                    }
                }
                zipFile.close();
            } catch (IOException e){
                fileReader.onReadError(e.getMessage());
                e.printStackTrace();
            }
        } else {
            fileReader.onReadError("File not found or file is NULL");
        }
    }
    public interface IFileReader{
        void onReadComplete(File file, SKIN_FORMAT skinFormat, Bitmap preview);
        void onReadError(String reason);
    }

}
