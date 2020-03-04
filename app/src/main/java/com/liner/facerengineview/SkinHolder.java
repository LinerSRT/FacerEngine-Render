package com.liner.facerengineview;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.InputStream;

public class SkinHolder {
    private String filePath;
    private Bitmap preview;

    public SkinHolder(String filePath, Bitmap preview) {
        this.filePath = filePath;
        this.preview = preview;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setPreview(Bitmap preview) {
        this.preview = preview;
    }
}
