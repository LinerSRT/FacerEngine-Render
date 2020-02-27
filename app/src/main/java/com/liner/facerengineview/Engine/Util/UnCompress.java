package com.liner.facerengineview.Engine.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnCompress {
    private static final int BUFFER = 2048;
    private String dest;
    private String mDestFolder;
    private OnCompleteListener mListener;
    private String mZipFile;

    public interface OnCompleteListener {
        void onComplete();
        void onError();
    }

    public UnCompress(String dest, String zipFile, OnCompleteListener mListener) {
        this.mDestFolder = dest;
        this.dest = dest;
        this.mZipFile = zipFile;
        this.mListener = mListener;
        if (zipFile == null) {
            mListener.onError();
        }
    }

    public void unzip() {
        if (dest != null) {
            File file = new File(dest);
            file.mkdir();
            new File(file, "images").mkdir();
            new File(file, "fonts").mkdir();
        }
        String resutName = "";
        if (mZipFile != null) {
            try {
                InputStream is = new FileInputStream(mZipFile);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
                byte[] buffer = new byte[BUFFER];
                while (true) {
                    ZipEntry ze = zis.getNextEntry();
                    if (ze == null) {
                        break;
                    }
                    String name = ze.getName();
                    if (ze.isDirectory()) {
                        new File(mDestFolder + name).mkdirs();
                    } else {
                        String[] finalName;
                        if (name.endsWith(".json") || name.endsWith("preview.png")) {
                            finalName = name.split("/");
                            resutName = finalName[finalName.length - 1];
                        } else if (name.contains("fonts")) {
                            finalName = name.split("/");
                            resutName = "fonts/" + finalName[finalName.length - 1];
                        } else if (name.contains("images")) {
                            finalName = name.split("/");
                            resutName = "images/" + finalName[finalName.length - 1];
                        }
                        FileOutputStream fout = new FileOutputStream(mDestFolder + resutName);
                        while (true) {
                            int count = zis.read(buffer);
                            if (count == -1) {
                                break;
                            }
                            fout.write(buffer, 0, count);
                        }
                        fout.close();
                        zis.closeEntry();
                    }
                }
                zis.close();
                is.close();
                if (mListener != null) {
                    mListener.onComplete();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}